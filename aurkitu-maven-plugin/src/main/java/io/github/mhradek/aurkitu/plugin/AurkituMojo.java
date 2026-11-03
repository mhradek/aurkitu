package io.github.mhradek.aurkitu.plugin;

import io.github.mhradek.aurkitu.plugin.core.SchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * Modern Aurkitu Maven Mojo (1.0.0).
 * Generates FlatBuffer schemas from annotated Java classes using ClassGraph.
 */
@Slf4j
@Mojo(name = "build-schema", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
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

    @Override
    public void execute() throws MojoExecutionException {
        log.info("Aurkitu 1.0.0 - generating schema '{}' in namespace '{}'", schemaName, schemaNamespace);

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new MojoExecutionException("Failed to create output directory: " + outputDirectory);
        }

        SchemaGenerator generator = new SchemaGenerator(project, specifiedPackages, validate);
        generator.generate(schemaName, schemaNamespace, outputDirectory);

        log.info("Schema generation complete. Output: {}", outputDirectory.getAbsolutePath());
    }
}
