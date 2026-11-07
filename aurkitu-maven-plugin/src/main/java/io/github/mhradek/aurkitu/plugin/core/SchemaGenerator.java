package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Orchestrates schema generation: scan the classpath, read the annotated classes into
 * a {@link Schema} model ({@link SchemaReader}), validate it ({@link SchemaValidator}),
 * then let the model write itself.
 */
@Slf4j
public class SchemaGenerator {

    private final Config config;

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
        /** Write boxed Java scalars (Integer, Boolean, ...) in tables as optional scalars ({@code = null}). */
        public final boolean optionalScalars;

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
            this(project, specifiedPackages, validate, ignoreStaticMembers, fileIdentifier, includes,
                    mapCommonTypes, cacheEnabled, snakeCaseFields, generateVersion, failOnValidationIssues,
                    fileExtension, classpathElements, false);
        }

        @SuppressWarnings("PMD.ExcessiveParameterList")
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate,
                      boolean ignoreStaticMembers, String fileIdentifier, List<String> includes,
                      boolean mapCommonTypes, boolean cacheEnabled, boolean snakeCaseFields,
                      boolean generateVersion, boolean failOnValidationIssues, String fileExtension,
                      List<String> classpathElements, boolean optionalScalars) {
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
            this.optionalScalars = optionalScalars;
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
        File outFile = new File(outputDir, schemaName + ".fbs");

        ClassGraph classGraph = new ClassGraph();
        if (config.classpathElements != null && !config.classpathElements.isEmpty()) {
            classGraph.overrideClasspath(config.classpathElements);
        }

        try (ScanResult scanResult = classGraph
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableFieldInfo()
                .enableMethodInfo()
                .ignoreFieldVisibility()
                .acceptPackages(config.specifiedPackages != null && !config.specifiedPackages.isEmpty()
                        ? config.specifiedPackages.toArray(new String[0])
                        : new String[]{"*"})
                .scan()) {

            Schema schema = new SchemaReader(config, TypeRegistry.build(scanResult, namespace)).read(scanResult);
            SchemaValidator.Result validation = new SchemaValidator(config.failOnValidationIssues).validate(schema);
            List<String> issues = config.validate ? validation.issues() : List.of();
            List<String> warnings = config.validate ? validation.warnings() : List.of();

            String output = validation.schema().write(AurkituVersion.schemaHeader(), config.generateVersion, issues, warnings);
            if (config.cacheEnabled && unchanged(outFile, output)) {
                // Keeps the file's timestamp, so downstream steps (e.g. flatc code generation) can skip work.
                log.info("Schema {} is unchanged; not rewritten", outFile.getAbsolutePath());
            } else {
                Files.writeString(outFile.toPath(), output);
                log.info("Wrote schema to {}", outFile.getAbsolutePath());
            }

            if (config.validate) {
                warnings.forEach(warning -> log.warn("Warning: {}", warning));
                if (issues.isEmpty()) {
                    log.info("Basic validation complete. No issues found.");
                } else {
                    issues.forEach(issue -> log.warn("Validation issue: {}", issue));
                    log.warn("Validation issues found ({}). See schema comments.", issues.size());
                    if (config.failOnValidationIssues) {
                        throw new SchemaGenerationException("Schema validation failed with " + issues.size() + " issue(s). See schema comments.");
                    }
                }
            }
        } catch (IOException e) {
            throw new SchemaGenerationException("Failed to generate schema", e);
        }
    }

    /** True if {@code file} already holds exactly {@code content}. */
    private static boolean unchanged(File file, String content) throws IOException {
        return file.isFile() && Files.readString(file.toPath()).equals(content);
    }
}
