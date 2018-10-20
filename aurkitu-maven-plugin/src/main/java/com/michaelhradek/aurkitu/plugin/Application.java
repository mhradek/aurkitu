package com.michaelhradek.aurkitu.plugin;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.core.*;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    @Parameter(property = Application.MOJO_NAME + ".consolidated-schemas", defaultValue = "true")
    private Boolean consolidatedSchemas;

    @Parameter(property = Application.MOJO_NAME + ".schema-namespace", defaultValue = "generated.flatbuffers")
    private String schemaNamespace;

    @Parameter(property = Application.MOJO_NAME + ".schema-includes")
    private List<String> schemaIncludes;

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

    @Parameter(property = Application.MOJO_NAME + ".use-schema-caching", defaultValue = "false")
    private Boolean useSchemaCaching;

    // allow static access to the log
    private static Log log;

    public void execute() throws MojoExecutionException {

        if (log == null)
            log = getLog();

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

        Schema schema = new Schema();
        schema.setNamespace(schemaNamespace);
        schema.setName(schemaName);
        schema.setFileExtension(fileExtension);
        schema.setFileIdentifier(schemaFileIdentifier);
        schema.setIncludes(schemaIncludes);
        schema.setGenerateVersion(generateVersion);

        if (useSchemaCaching && Utilities.isSchemaPresent(schema, outputDirectory)) {
            log.info("Schema found & caching was requested; skipping schema update.");
            return;
        }

        ArtifactReference reference = new ArtifactReference(project, repoSystem, repoSession, repositories, specifiedDependencies);

        Processor processor =
            new Processor()
                .withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class)
                .withArtifactReference(reference)
                    .withNamespaceOverrideMap(namespaceOverrideMap)
                    .withSpecifiedDependencies(specifiedDependencies)
                    .withConsolidatedSchemas(consolidatedSchemas);

        schema = processor.buildSchema(schema);

        if (validateSchema) {
            Validator validator = new Validator().withSchema(schema);
            validator.validateSchema();
            schema.setIsValidSchema(validator.getErrors().isEmpty());
            schema.setValidator(validator);
            Application.getLogger().info(validator.getErrorComments());

            if (consolidatedSchemas != null && !consolidatedSchemas) {
                for (Schema dependencySchema : processor.getDepedencySchemas().values()) {
                    validator = new Validator().withSchema(dependencySchema);
                    validator.validateSchema();
                    dependencySchema.setIsValidSchema(validator.getErrors().isEmpty());
                    dependencySchema.setValidator(validator);
                    Application.getLogger().info(validator.getErrorComments());
                }
            }
        }

        if (outputDirectory == null) {
            log.debug("outputDirectory is NULL");
        } else {
            log.debug("outputDirectory is: " + outputDirectory);
        }

        FileGeneration fg = new FileGeneration(outputDirectory);
        try {
            fg.writeSchema(schema);

            if (consolidatedSchemas != null && !consolidatedSchemas) {
                for (Schema dependencySchema : processor.getDepedencySchemas().values()) {
                    fg.writeSchema(dependencySchema);
                }
            }
        } catch (IOException e) {
            log.error("Unable to write schemas to disk", e);
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
