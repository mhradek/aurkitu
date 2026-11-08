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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * {@code aurkitu:build-schema}: writes a FlatBuffers schema ({@code <schemaName>.fbs}) from the annotated
 * classes on the project's compile classpath, including those in dependencies. Runs in
 * {@code process-classes}, after compilation, so the classes can be scanned.
 *
 * <p>With {@code validate} (the default), schema problems are written as comments at the end of the schema
 * and logged; parts {@code flatc} would reject are left out either way, so the schema compiles.</p>
 */
@Slf4j
@Mojo(name = "build-schema", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class AurkituMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** The schema's file name, without {@code .fbs}. */
    @Parameter(property = "aurkitu.schemaName", required = true)
    private String schemaName;

    /** The schema's {@code namespace}, used for every type without its own {@code namespace}. */
    @Parameter(property = "aurkitu.schemaNamespace", defaultValue = "generated.flatbuffers")
    private String schemaNamespace;

    /** Where the schema is written. Created if missing. */
    @Parameter(property = "aurkitu.outputDir", defaultValue = "${project.build.directory}/aurkitu/schemas")
    private File outputDirectory;

    /**
     * Packages to scan, including their subpackages. Empty to scan the whole classpath; naming the
     * model packages is faster and keeps annotated classes in dependencies out unless listed.
     */
    @Parameter(property = "aurkitu.specifiedPackages")
    private List<String> specifiedPackages;

    /**
     * Report schema problems as comments at the end of the schema and in the log. Off, they are not
     * reported, but the invalid parts are still left out.
     */
    @Parameter(property = "aurkitu.validate", defaultValue = "true")
    private boolean validate;

    /** Leave {@code static} fields out of tables and structs. */
    @Parameter(property = "aurkitu.ignoreStaticMembers", defaultValue = "true")
    private boolean ignoreStaticMembers;

    /**
     * Writes {@code file_identifier "XXXX"}: four characters stored in each buffer whose root is the
     * root type, so readers can check what a buffer holds. Other lengths are reported.
     */
    @Parameter(property = "aurkitu.fileIdentifier")
    private String fileIdentifier;

    /**
     * Schema files to {@code include}, e.g. {@code common.fbs}. Types they declare can be referenced, so
     * references to types not declared in this schema are no longer checked.
     */
    @Parameter(property = "aurkitu.includes")
    private List<String> includes;

    /**
     * Write JDK value types ({@code UUID}, {@code BigDecimal}, {@code URI}, {@code Date} and the
     * {@code java.time} types) as {@code string}. Off, fields of these types are reported and left out.
     */
    @Parameter(property = "aurkitu.mapCommonTypes", defaultValue = "true")
    private boolean mapCommonTypes;

    /**
     * Optional path to the flatc binary. When set and validate=true, the schema is compiled with flatc after
     * it is written, and the build fails if flatc rejects it.
     */
    @Parameter(property = "aurkitu.flatcPath")
    private String flatcPath;

    /**
     * When true, the schema file is only rewritten when its content changes, so its timestamp is kept
     * and downstream steps (e.g. flatc code generation) can skip work. Validation always runs.
     */
    @Parameter(property = "aurkitu.cacheEnabled", defaultValue = "false")
    private boolean cacheEnabled;

    /**
     * When true (default), convert Java field names to snake_case in the schema ({@code userId} as
     * {@code user_id}), the FlatBuffers style.
     */
    @Parameter(property = "aurkitu.snakeCaseFields", defaultValue = "true")
    private boolean snakeCaseFields;

    /**
     * With snakeCaseFields, keep a run of capitals as one word: {@code userID} becomes {@code user_id} and
     * {@code XMLParser} becomes {@code xml_parser}, instead of {@code user_i_d} and {@code x_m_l_parser}.
     * Off by default, because it renames existing fields.
     *
     * @since 1.2.0
     */
    @Parameter(property = "aurkitu.snakeCaseAcronyms", defaultValue = "false")
    private boolean snakeCaseAcronyms;

    /**
     * Write map keys with their own type ({@code Map<Integer, V>} gets {@code key:int}), marked {@code (key)} so
     * generated code can look entries up, instead of always {@code key:string}. Off by default, because it changes
     * existing map tables.
     *
     * @since 1.2.0
     */
    @Parameter(property = "aurkitu.typedMapKeys", defaultValue = "false")
    private boolean typedMapKeys;

    /**
     * When true, write a hash of the schema as a {@code // @version:} comment at the top of the file. It
     * changes when the schema does; validation comments are not part of it.
     */
    @Parameter(property = "aurkitu.generateVersion", defaultValue = "false")
    private boolean generateVersion;

    /**
     * When true and validate is true, fail the build if any validation issues are found. Warnings
     * (risks to existing data, not errors) never fail it. The schema is written either way.
     */
    @Parameter(property = "aurkitu.failOnValidationIssues", defaultValue = "false")
    private boolean failOnValidationIssues;

    /**
     * When true, boxed Java scalars (Integer, Boolean, ...) in tables are written as optional scalars
     * ({@code = null}), so an unset value reads as null rather than 0. Off by default.
     *
     * @since 1.1.0
     */
    @Parameter(property = "aurkitu.optionalScalars", defaultValue = "false")
    private boolean optionalScalars;

    /**
     * Optional extension for binary files of this schema written by flatc, e.g. {@code dat} (emits
     * file_extension "..."). flatc's default is {@code bin}.
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
                resolveClasspath(), optionalScalars)
                .withSnakeCaseAcronyms(snakeCaseAcronyms)
                .withTypedMapKeys(typedMapKeys);
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

    /**
     * Validates the schema by compiling it to a binary schema ({@code flatc --binary --schema}),
     * which parses and checks it fully without targeting a language. Output is discarded.
     */
    private void runFlatcValidation(File schemaFile) throws MojoExecutionException {
        Path scratch = null;
        try {
            scratch = Files.createTempDirectory("aurkitu-flatc");
            ProcessBuilder pb = new ProcessBuilder(flatcPath, "--binary", "--schema",
                    "-o", scratch.toString(), schemaFile.getAbsolutePath());
            pb.directory(schemaFile.getParentFile());
            pb.redirectErrorStream(true);
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
        } finally {
            deleteQuietly(scratch);
        }
    }

    private static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // best effort
        }
    }
}
