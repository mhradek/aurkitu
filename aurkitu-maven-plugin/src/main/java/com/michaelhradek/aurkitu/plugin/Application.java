package com.michaelhradek.aurkitu.plugin;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.core.FileGeneration;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author m.hradek
 *
 */
@Mojo(name = Application.MOJO_GOAL, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
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

    @Parameter(property = Application.MOJO_NAME + ".ouput-dir", defaultValue = "${project.build.directory}/aurkitu/schemas")
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

    @Parameter(property = Application.MOJO_NAME + ".use-schema-caching", defaultValue = "false")
    private Boolean useSchemaCaching;

    // allow static access to the log
    private static Log log;

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
    @TestOnly
    private void log() {

        getLogger().info("execute: " + Application.MOJO_NAME);

        if (project != null) {
            getLogger().info(" MavenProject (ArtifactId): " + project.getArtifactId());
            getLogger().info(" MavenProject (GroupId): " + project.getGroupId());
        }

        getLogger().info(" schemaNamespace: " + schemaNamespace);
        getLogger().info(" schemaName: " + schemaName);
        getLogger().info(" fileExtension: " + fileExtension);
        getLogger().info(" schemaFileIdentifier: " + schemaFileIdentifier);
        getLogger().info(" outputDirectory: " + outputDirectory.getAbsolutePath());
        getLogger().info(" validateSchema: " + validateSchema);
        getLogger().info(" generateVersion: " + generateVersion);
        getLogger().info(" namespaceOverrideMap: " + (namespaceOverrideMap == null ? "null" : namespaceOverrideMap.toString()));
        getLogger().info(" useSchemaCaching: " + useSchemaCaching);
        getLogger().info(" schemaIncludes: " + (schemaIncludes == null ? "null" : schemaIncludes.toString()));
        getLogger().info(" specifiedDependencies: " + (specifiedDependencies == null ? "null" : specifiedDependencies.toString()));
        getLogger().info(" consolidatedSchemas: " + consolidatedSchemas);
    }

    /**
     * @param reference ArtifactReference is how we move around the MavenProject and objects required for annotation finding
     * @return All the schemas required for processing
     * @throws MojoExecutionException if anything goes wrong
     */
    private List<Schema> setup(ArtifactReference reference) throws MojoExecutionException {
        Schema schema = new Schema();
        schema.setNamespace(schemaNamespace);
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
                getLogger().debug("Dependencies found: " + classpathReferenceList.size());
                for (ClasspathReference classpathReference : classpathReferenceList) {
                    Schema dependencySchema = new Schema();
                    getLogger().debug(" namespace: " + classpathReference.getDerivedNamespace());
                    dependencySchema.setName(classpathReference.getArtifact());
                    dependencySchema.setNamespace(classpathReference.getDerivedNamespace());
                    dependencySchema.setClasspathReferenceList(Arrays.asList(classpathReference));
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
            getLogger().debug("outputDirectory is NULL");
        } else {
            getLogger().debug("outputDirectory is: " + outputDirectory);
        }

        FileGeneration fg = new FileGeneration(outputDirectory);
        try {
            for (Schema completeSchema : processedSchemas) {
                fg.writeSchema(completeSchema);
            }
        } catch (IOException e) {
            getLogger().error("Unable to write schemas to disk", e);
        }
    }

    /**
     * @return the application logger defaulting to {@link}SystemStreamLog
     */
    public static Log getLogger() {
        if (log == null) {
            log = new SystemStreamLog();
        }

        return log;
    }
}
