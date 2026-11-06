package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates schema generation. Scanning, type mapping, field/table/enum
 * emission, and caching are delegated to focused classes in this package.
 */
@Slf4j
public class SchemaGenerator {

    private final Config config;
    private final Map<String, String> generatedMapTables = new LinkedHashMap<>();
    private final List<String> validationIssues = new ArrayList<>();

    public static class Config {
        public final MavenProject project;
        public final List<String> specifiedPackages;
        public final boolean validate;
        public final boolean ignoreStaticMembers;
        public final String fileIdentifier;
        public final List<String> includes;
        public final boolean mapCommonTypes;
        public final boolean cacheEnabled;
        public final boolean snakeCaseFields;
        public final boolean generateVersion;
        public final boolean failOnValidationIssues;
        public final String fileExtension;
        /**
         * Classpath to scan (e.g. the project's compile classpath). When null or empty,
         * ClassGraph falls back to the current classloader, which inside Maven is the
         * plugin's own classpath and does not contain the project's classes.
         */
        public final List<String> classpathElements;

        @SuppressWarnings("PMD.ExcessiveParameterList")
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate,
                      boolean ignoreStaticMembers, String fileIdentifier, List<String> includes,
                      boolean mapCommonTypes, boolean cacheEnabled, boolean snakeCaseFields,
                      boolean generateVersion, boolean failOnValidationIssues, String fileExtension) {
            this(project, specifiedPackages, validate, ignoreStaticMembers, fileIdentifier, includes,
                    mapCommonTypes, cacheEnabled, snakeCaseFields, generateVersion, failOnValidationIssues,
                    fileExtension, null);
        }

        @SuppressWarnings("PMD.ExcessiveParameterList")
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate,
                      boolean ignoreStaticMembers, String fileIdentifier, List<String> includes,
                      boolean mapCommonTypes, boolean cacheEnabled, boolean snakeCaseFields,
                      boolean generateVersion, boolean failOnValidationIssues, String fileExtension,
                      List<String> classpathElements) {
            this.project = project;
            this.specifiedPackages = specifiedPackages;
            this.validate = validate;
            this.ignoreStaticMembers = ignoreStaticMembers;
            this.fileIdentifier = fileIdentifier;
            this.includes = includes != null ? includes : List.of();
            this.mapCommonTypes = mapCommonTypes;
            this.cacheEnabled = cacheEnabled;
            this.snakeCaseFields = snakeCaseFields;
            this.generateVersion = generateVersion;
            this.failOnValidationIssues = failOnValidationIssues;
            this.fileExtension = fileExtension;
            this.classpathElements = classpathElements;
        }

        // Convenience constructor for tests
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate) {
            this(project, specifiedPackages, validate, true, null, List.of(), true, false, true, false, false, null);
        }
    }

    public SchemaGenerator(Config config) {
        this.config = config;
    }

    // Backward-compatible constructor
    public SchemaGenerator(MavenProject project, List<String> specifiedPackages, boolean validate) {
        this(new Config(project, specifiedPackages, validate));
    }

    public void generate(String schemaName, String namespace, File outputDir) {
        generatedMapTables.clear();
        validationIssues.clear();

        File outFile = new File(outputDir, schemaName + ".fbs");
        File cacheFile = new File(outputDir, schemaName + ".fbs.cache");

        ClassGraph classGraph = new ClassGraph();
        if (config.classpathElements != null && !config.classpathElements.isEmpty()) {
            classGraph.overrideClasspath(config.classpathElements);
        }

        try (ScanResult scanResult = classGraph
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableFieldInfo()
                .ignoreFieldVisibility()
                .acceptPackages(config.specifiedPackages != null && !config.specifiedPackages.isEmpty()
                        ? config.specifiedPackages.toArray(new String[0])
                        : new String[]{"*"})
                .scan()) {

            String inputHash = SchemaCache.computeHash(config, namespace, scanResult);
            if (SchemaCache.isHit(config.cacheEnabled, outFile, cacheFile, inputHash)) {
                log.info("Schema cache hit for {}; skipping regeneration", schemaName);
                return;
            }

            TypeRegistry registry = TypeRegistry.build(scanResult, namespace, config.validate, validationIssues);
            TypeMapper typeMapper = new TypeMapper(config, generatedMapTables, registry);
            FieldEmitter fieldEmitter = new FieldEmitter(config, typeMapper, validationIssues);
            TableEmitter tableEmitter = new TableEmitter(config, fieldEmitter, validationIssues);
            EnumEmitter enumEmitter = new EnumEmitter(config.validate, validationIssues);

            StringBuilder schema = new StringBuilder();
            schema.append(AurkituVersion.schemaHeader());

            // flatc requires includes before any other declaration.
            for (String include : config.includes) {
                schema.append("include \"").append(include).append("\";\n");
            }
            if (!config.includes.isEmpty()) {
                schema.append("\n");
            }

            schema.append("namespace ").append(namespace).append(";\n\n");

            if (config.fileIdentifier != null && !config.fileIdentifier.isBlank()) {
                if (config.fileIdentifier.length() != 4 && config.validate) {
                    validationIssues.add("file_identifier \"" + config.fileIdentifier + "\" must be exactly 4 characters");
                }
                schema.append("file_identifier \"").append(config.fileIdentifier).append("\";\n\n");
            }
            if (config.fileExtension != null && !config.fileExtension.isBlank()) {
                schema.append("file_extension \"").append(config.fileExtension).append("\";\n\n");
            }

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
                appendInNamespace(schema, registry, classInfo, enumEmitter.buildEnum(classInfo));
            }

            ClassInfo rootType = null;
            for (ClassInfo classInfo : TableEmitter.declarationOrder(scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName()))) {
                appendInNamespace(schema, registry, classInfo, tableEmitter.buildTable(classInfo));
                if (TableEmitter.isRootType(classInfo)) {
                    if (rootType != null) {
                        log.warn("Multiple root types detected; using last one: {}", classInfo.getName());
                        if (config.validate) {
                            validationIssues.add("Multiple root types; last used: " + TypeRegistry.typeName(classInfo.getName()));
                        }
                    }
                    rootType = classInfo;
                }
            }

            for (String mapTable : generatedMapTables.values()) {
                schema.append(mapTable);
            }

            if (rootType != null) {
                schema.append("root_type ").append(registry.reference(rootType.getName(), namespace)).append(";\n");
            } else if (config.validate) {
                validationIssues.add("No root type defined");
            }

            if (config.validate && !validationIssues.isEmpty()) {
                schema.append("\n// Validation issues:\n");
                for (String issue : validationIssues) {
                    schema.append("// - ").append(issue).append("\n");
                }
            }

            String output = schema.toString();
            if (config.generateVersion) {
                String header = AurkituVersion.schemaHeader();
                output = output.replace(header, header + "// @version: " + contentVersion(output) + "\n");
            }

            Files.writeString(outFile.toPath(), output);
            if (config.cacheEnabled) {
                SchemaCache.write(cacheFile, inputHash);
            }
            log.info("Wrote schema to {}", outFile.getAbsolutePath());

            if (config.validate) {
                if (validationIssues.isEmpty()) {
                    log.info("Basic validation complete. No issues found.");
                } else {
                    log.warn("Validation issues found ({}). See schema comments.", validationIssues.size());
                    if (config.failOnValidationIssues) {
                        throw new SchemaGenerationException("Schema validation failed with " + validationIssues.size() + " issue(s). See schema comments.");
                    }
                }
            }
        } catch (IOException e) {
            throw new SchemaGenerationException("Failed to generate schema", e);
        }
    }

    /**
     * Places a type declaration in its namespace. A {@code namespace} line applies to every
     * declaration after it, so the default namespace is restored straight after an override.
     */
    private static void appendInNamespace(StringBuilder schema, TypeRegistry registry, ClassInfo classInfo, String declaration) {
        String ns = registry.namespaceOf(classInfo.getName());
        if (ns.equals(registry.defaultNamespace())) {
            schema.append(declaration);
        } else {
            schema.append("namespace ").append(ns).append(";\n\n")
                    .append(declaration)
                    .append("namespace ").append(registry.defaultNamespace()).append(";\n\n");
        }
    }

    /**
     * Hash of the schema body only: the header line (which carries the plugin version) and the
     * trailing validation comments are excluded, so the version changes only when the schema does.
     */
    static String contentVersion(String schema) {
        String body = schema.substring(schema.indexOf('\n') + 1);
        int validation = body.indexOf("\n// Validation issues:");
        if (validation >= 0) {
            body = body.substring(0, validation);
        }
        return Integer.toHexString(body.hashCode());
    }
}
