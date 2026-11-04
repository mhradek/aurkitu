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

        @SuppressWarnings("PMD.ExcessiveParameterList")
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate,
                      boolean ignoreStaticMembers, String fileIdentifier, List<String> includes,
                      boolean mapCommonTypes, boolean cacheEnabled, boolean snakeCaseFields,
                      boolean generateVersion, boolean failOnValidationIssues, String fileExtension) {
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

        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableFieldInfo()
                .enableStaticFinalFieldConstantInitializerValues()
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

            TypeMapper typeMapper = new TypeMapper(config, generatedMapTables);
            FieldEmitter fieldEmitter = new FieldEmitter(config, typeMapper, validationIssues);
            TableEmitter tableEmitter = new TableEmitter(config, fieldEmitter, validationIssues);

            StringBuilder schema = new StringBuilder();
            schema.append("// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n");
            schema.append("namespace ").append(namespace).append(";\n\n");

            if (config.fileIdentifier != null && !config.fileIdentifier.isBlank()) {
                schema.append("file_identifier \"").append(config.fileIdentifier).append("\";\n\n");
            }
            if (config.fileExtension != null && !config.fileExtension.isBlank()) {
                schema.append("file_extension \"").append(config.fileExtension).append("\";\n\n");
            }

            for (String include : config.includes) {
                schema.append("include \"").append(include).append("\";\n");
            }
            if (!config.includes.isEmpty()) {
                schema.append("\n");
            }

            String rootType = null;

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
                schema.append(EnumEmitter.buildEnum(classInfo));
            }

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName())) {
                schema.append(tableEmitter.buildTable(classInfo));
                if (TableEmitter.isRootType(classInfo)) {
                    if (rootType != null) {
                        log.warn("Multiple root types detected; using last one: {}", classInfo.getSimpleName());
                        if (config.validate) {
                            validationIssues.add("Multiple root types; last used: " + classInfo.getSimpleName());
                        }
                    }
                    rootType = classInfo.getSimpleName();
                }
            }

            for (String mapTable : generatedMapTables.values()) {
                schema.append(mapTable);
            }

            if (rootType != null) {
                schema.append("root_type ").append(rootType).append(";\n");
            } else if (config.validate) {
                validationIssues.add("No root type defined");
            }

            if (config.validate && !validationIssues.isEmpty()) {
                schema.append("\n// Validation issues:\n");
                for (String issue : validationIssues) {
                    schema.append("// - ").append(issue).append("\n");
                }
            }

            String sanitized = schema.toString().replace('$', '_');

            if (config.generateVersion) {
                String forHash = sanitized;
                int valIdx = forHash.indexOf("\n// Validation issues:");
                if (valIdx > 0) {
                    forHash = forHash.substring(0, valIdx);
                }
                String version = Integer.toHexString(forHash.hashCode());
                sanitized = sanitized.replace(
                        "// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n",
                        "// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n// @version: " + version + "\n");
            }

            Files.writeString(outFile.toPath(), sanitized);
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
}
