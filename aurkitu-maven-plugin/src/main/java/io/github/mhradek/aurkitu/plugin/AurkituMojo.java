package io.github.mhradek.aurkitu.plugin;

import io.github.mhradek.aurkitu.plugin.core.AurkituVersion;
import io.github.mhradek.aurkitu.plugin.core.SchemaGenerationException;
import io.github.mhradek.aurkitu.plugin.core.SchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Aurkitu Maven Mojo.
 * Generates FlatBuffer schemas from annotated Java classes using ClassGraph.
 */
@Slf4j
@Mojo(name = "build-schema", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class AurkituMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "aurkitu.schemaName", required = true)
    private String schemaName;

    @Parameter(property = "aurkitu.schemaNamespace", defaultValue = "generated.flatbuffers")
    private String schemaNamespace;

    @Parameter(property = "aurkitu.outputDir", defaultValue = "${project.build.directory}/aurkitu/schemas")
    private File outputDirectory;

    @Parameter(property = "aurkitu.specifiedPackages")
    private List<String> specifiedPackages;

    @Parameter(property = "aurkitu.validate", defaultValue = "true")
    private boolean validate;

    @Parameter(property = "aurkitu.ignoreStaticMembers", defaultValue = "true")
    private boolean ignoreStaticMembers;

    @Parameter(property = "aurkitu.fileIdentifier")
    private String fileIdentifier;

    @Parameter(property = "aurkitu.includes")
    private List<String> includes;

    @Parameter(property = "aurkitu.mapCommonTypes", defaultValue = "true")
    private boolean mapCommonTypes;

    /**
     * Optional path to the flatc binary. When set and validate=true, the generated schema is validated with flatc.
     */
    @Parameter(property = "aurkitu.flatcPath")
    private String flatcPath;

    /**
     * When true, skip regeneration if the output schema already exists and the input hash matches.
     */
    @Parameter(property = "aurkitu.cacheEnabled", defaultValue = "false")
    private boolean cacheEnabled;

    /**
     * When true (default), convert Java field names to snake_case in the schema.
     */
    @Parameter(property = "aurkitu.snakeCaseFields", defaultValue = "true")
    private boolean snakeCaseFields;

    /**
     * When true, compute a version hash of the schema (excluding validation comments)
     * and emit it as a comment at the top of the file.
     */
    @Parameter(property = "aurkitu.generateVersion", defaultValue = "false")
    private boolean generateVersion;

    /**
     * When true and validate is true, fail the build if any validation issues are found.
     */
    @Parameter(property = "aurkitu.failOnValidationIssues", defaultValue = "false")
    private boolean failOnValidationIssues;

    /**
     * Optional file extension for generated binaries (emits file_extension "...").
     */
    @Parameter(property = "aurkitu.fileExtension")
    private String fileExtension;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Aurkitu {} - generating schema '{}' in namespace '{}'", AurkituVersion.VERSION, schemaName, schemaNamespace);

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
        }

        SchemaGenerator.Config config = new SchemaGenerator.Config(
                project, specifiedPackages, validate, ignoreStaticMembers, fileIdentifier, includes, mapCommonTypes, cacheEnabled, snakeCaseFields, generateVersion, failOnValidationIssues, fileExtension,
                resolveClasspath());
        SchemaGenerator generator = new SchemaGenerator(config);
        try {
            generator.generate(schemaName, schemaNamespace, outputDirectory);
        } catch (SchemaGenerationException e) {
            // Invalid models or failed validation are build failures, not plugin errors.
            throw new MojoFailureException(e.getMessage(), e);
        }

        log.info("Schema generation complete. Output: {}", outputDirectory.getAbsolutePath());

        if (validate && flatcPath != null && !flatcPath.isBlank()) {
            runFlatcValidation(new File(outputDirectory, schemaName + ".fbs"));
        }
    }

    /**
     * The project's compile classpath (its output directory plus compile-scope dependencies).
     * Returns null when no project is bound, in which case the current classloader is scanned.
     */
    private List<String> resolveClasspath() throws MojoExecutionException {
        if (project == null) {
            return null;
        }
        try {
            List<String> elements = project.getCompileClasspathElements();
            log.debug("Scanning project compile classpath: {}", elements);
            return elements;
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to resolve project compile classpath", e);
        }
    }

    private void runFlatcValidation(File schemaFile) throws MojoExecutionException {
        ProcessBuilder pb = new ProcessBuilder(flatcPath, "--schema", schemaFile.getAbsolutePath());
        pb.directory(schemaFile.getParentFile());
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            // Drain output before waiting so a full pipe cannot block flatc.
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new MojoExecutionException("flatc validation failed for " + schemaFile.getName() + ":\n" + output);
            }
            log.info("flatc validation succeeded for {}", schemaFile.getName());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to run flatc", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while running flatc", e);
        }
    }
}
