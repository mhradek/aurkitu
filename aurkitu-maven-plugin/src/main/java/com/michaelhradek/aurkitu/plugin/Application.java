package com.michaelhradek.aurkitu.plugin;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.core.FileGeneration;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author m.hradek
 */
@Slf4j
@Mojo(name = Application.MOJO_GOAL, defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class Application extends AbstractMojo {

    public static final String MOJO_NAME = "aurkitu-maven-plugin";
    public static final String MOJO_GOAL = "build-schema";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repositories;

    @Parameter(property = Application.MOJO_NAME + ".output-dir", defaultValue = "${project.build.directory}/aurkitu/schemas")
    private File outputDirectory;

    @Parameter(property = Application.MOJO_NAME + ".specified-dependencies")
    private List<String> specifiedDependencies;

    @Parameter(property = Application.MOJO_NAME + ".schema-namespace", defaultValue = "generated.flatbuffers")
    private String schemaNamespace;

    @Parameter(property = Application.MOJO_NAME + ".schema-includes")
    private Set<String> schemaIncludes;

    @Parameter(property = Application.MOJO_NAME + ".validate-schema", defaultValue = "true")
    private Boolean validateSchema;

    @Parameter(property = Application.MOJO_NAME + ".schema-name", required = true)
    private String schemaName;

    @Parameter(property = Application.MOJO_NAME + ".schema-file-identifier")
    private String schemaFileIdentifier;

    @Parameter(property = Application.MOJO_NAME + ".flatc-extention")
    private String fileExtension;

    @Parameter(property = Application.MOJO_NAME + ".namespace-override-map")
    private Map<String, String> namespaceOverrideMap;

    @Parameter(property = Application.MOJO_NAME + ".generate-version", defaultValue = "false")
    private Boolean generateVersion;

    @Parameter(property = Application.MOJO_NAME + ".consolidated-schemas", defaultValue = "true")
    private Boolean consolidatedSchemas;

    // Needs thought
    // @Parameter(property = Application.MOJO_NAME + ".build-base-schemas", defaultValue = "true")
    // private Boolean buildDependencySchemas;

    @Parameter(property = Application.MOJO_NAME + ".use-schema-caching", defaultValue = "false")
    private Boolean useSchemaCaching;

    /**
     * @throws MojoExecutionException if anything goes wrong
     */
    public void execute() throws MojoExecutionException {

        // Log
        log();

        ArtifactReference reference = new ArtifactReference(project, repoSystem, repoSession, repositories, specifiedDependencies);

        // Setup
        List<Schema> schemas = setup(reference);

        // Eh, cache?
        if (useSchemaCaching && Utilities.areSchemasPresent(schemas, outputDirectory)) {
            log.info("Schema found & caching was requested; skipping schema update.");
            return;
        }

        // Parse
        final Processor processor = parse(schemas, reference);

        // Write files
        write(processor.getProcessedSchemas());
    }

    /**
     * Log everything sent to this plugin via the configuration from the Maven POM.
     */
    private void log() {

        log.info("execute: " + Application.MOJO_NAME);

        if (project != null) {
            log.info(" MavenProject (ArtifactId): " + project.getArtifactId());
            log.info(" MavenProject (GroupId): " + project.getGroupId());
        }

        log.info(" schemaNamespace: " + schemaNamespace);
        log.info(" schemaName: " + schemaName);
        log.info(" fileExtension: " + fileExtension);
        log.info(" schemaFileIdentifier: " + schemaFileIdentifier);
        log.info(" outputDirectory: " + outputDirectory.getAbsolutePath());
        log.info(" validateSchema: " + validateSchema);
        log.info(" generateVersion: " + generateVersion);
        log.info(" namespaceOverrideMap: " + (namespaceOverrideMap == null ? "null" : namespaceOverrideMap.toString()));
        log.info(" useSchemaCaching: " + useSchemaCaching);
        log.info(" schemaIncludes: " + (schemaIncludes == null ? "null" : schemaIncludes.toString()));
        log.info(" specifiedDependencies: " + (specifiedDependencies == null ? "null" : specifiedDependencies.toString()));
        log.info(" consolidatedSchemas: " + consolidatedSchemas);
    }

    /**
     * @param reference ArtifactReference is how we move around the MavenProject and objects required for annotation finding
     * @return All the schemas required for processing
     * @throws MojoExecutionException if anything goes wrong
     */
    private List<Schema> setup(ArtifactReference reference) throws MojoExecutionException {
        Schema schema = new Schema();
        schema.setNamespace(schemaNamespace);
        if (schema.getNamespace() == null || schema.getNamespace().isEmpty()) {
            schema.setNamespace(
                    new Namespace(
                            reference.getMavenProject().getGroupId(),
                            Config.SCHEMA_NAMESPACE_IDENTIFIER_DEFAULT,
                            reference.getMavenProject().getArtifactId()
                    )
            );
        }

        schema.setName(schemaName);
        schema.setFileExtension(fileExtension);
        schema.setFileIdentifier(schemaFileIdentifier);
        schema.setIncludes(schemaIncludes);
        schema.setGenerateVersion(generateVersion);

        List<Schema> dependencySchemas = new ArrayList<>();

        // Set up the class paths
        try {
            schema.setClasspathReferenceList(Utilities.buildProjectClasspathList(reference,
                    ClasspathSearchType.BOTH));

            if (consolidatedSchemas != null && !consolidatedSchemas) {
                List<ClasspathReference> classpathReferenceList = Utilities.buildProjectClasspathList(reference,
                        ClasspathSearchType.DEPENDENCIES);
                log.debug("Dependencies found: " + classpathReferenceList.size());
                for (ClasspathReference classpathReference : classpathReferenceList) {
                    Schema dependencySchema = new Schema();
                    log.debug(" namespace: " + classpathReference.getNamespace().toString());

                    Namespace dependencyNamespace = classpathReference.getNamespace();
                    if (!schema.getNamespace().isEmpty() && schema.getNamespace().getIdentifier() != null) {
                        dependencyNamespace.setIdentifier(schema.getNamespace().getIdentifier());
                    } else {
                        dependencyNamespace.setIdentifier(Config.SCHEMA_NAMESPACE_IDENTIFIER_DEFAULT);
                    }

                    dependencySchema.setName(classpathReference.getArtifactId());
                    dependencySchema.setNamespace(dependencyNamespace);
                    dependencySchema.setClasspathReferenceList(new ArrayList<>(Arrays.asList(classpathReference)));
                    dependencySchema.setDependency(true);
                    dependencySchemas.add(dependencySchema);
                }
            }
        } catch (IOException | DependencyResolutionRequiredException | ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }

        // The only differentiator is the setDependency flag
        List<Schema> candidateSchemas = new ArrayList<>();
        candidateSchemas.add(schema);
        candidateSchemas.addAll(dependencySchemas);
        return candidateSchemas;
    }

    /**
     * @param candidateSchemas The schemas we wish to examine
     * @param reference        the ArtifactReference object
     * @return a processor that's been executed
     * @throws MojoExecutionException if anything goes wrong
     */
    private Processor parse(List<Schema> candidateSchemas, ArtifactReference reference) throws MojoExecutionException {

        // Setup the processor
        Processor processor =
                new Processor()
                        .withSourceAnnotation(FlatBufferTable.class)
                        .withSourceAnnotation(FlatBufferEnum.class)
                        .withArtifactReference(reference)
                        .withNamespaceOverrideMap(namespaceOverrideMap)
                        .withSpecifiedDependencies(specifiedDependencies)
                        .withConsolidatedSchemas(consolidatedSchemas)
                        .withValidateSchemas(validateSchema);

        // Add schemas
        processor.addAllSchemas(candidateSchemas);

        // Process schemas
        processor.execute();

        return processor;
    }

    /**
     * @param processedSchemas schemas to write to disk
     */
    private void write(List<Schema> processedSchemas) {
        if (outputDirectory == null) {
            log.debug("outputDirectory is NULL");
        } else {
            log.debug("outputDirectory is: " + outputDirectory);
        }

        FileGeneration fg = new FileGeneration(outputDirectory);
        try {
            for (Schema completeSchema : processedSchemas) {
                fg.writeSchema(completeSchema);
            }
        } catch (IOException e) {
            log.error("Unable to write schemas to disk", e);
        }
    }
}
