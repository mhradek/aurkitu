/**
 *
 */
package com.michaelhradek.aurkitu;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.ArtifactReference;
import com.michaelhradek.aurkitu.core.FileGeneration;
import com.michaelhradek.aurkitu.core.Processor;
import com.michaelhradek.aurkitu.core.Validator;
import com.michaelhradek.aurkitu.core.output.Schema;

/**
 * @author m.hradek
 *
 */
@Mojo(name = Application.MOJO_GOAL, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Application extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repositories;

    public static final String MOJO_NAME = "aurkitu-maven-plugin";

    public static final String MOJO_GOAL = "build-schema";

    @Parameter(property = Application.MOJO_NAME + ".ouput-dir", defaultValue = "${project.build.directory}/aurkitu/schemas")
    private File outputDirectory;

    @Parameter(property = Application.MOJO_NAME + ".search-path", defaultValue = ".*")
    private String searchPath;

    @Parameter(property = Application.MOJO_NAME + ".schema-namespace", defaultValue = "generated.flatbuffers")
    private String schemaNamespace;

    @Parameter(property = Application.MOJO_NAME + ".schema-includes")
    private List<String> schemaIncludes;

    @Parameter(property = Application.MOJO_NAME + ".validate-schema", defaultValue = "true")
    private Boolean validateSchema;

    @Parameter(property = Application.MOJO_NAME + ".schema-name", required = true)
    private String schemaName;

    @Parameter(property = Application.MOJO_NAME + ".schema-file-identifier", defaultValue = "")
    private String fileIdentifier;

    @Parameter(property = Application.MOJO_NAME + ".flatc-extention", defaultValue = "")
    private String fileExtension;

    // allow static access to the log
    private static Log log;

    public void execute() throws MojoExecutionException, MojoFailureException {

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
        log.info(" fileIdentifier: " + fileIdentifier);
        log.info(" outputDirectory: " + outputDirectory.getAbsolutePath());
        log.info(" searchPath: " + searchPath);
        log.info(" validateSchema: " + validateSchema);

        ArtifactReference reference = new ArtifactReference(project, repoSystem, repoSession, repositories);

        Processor processor =
                new Processor().withSourceAnnotation(FlatBufferTable.class).withSourceAnnotation(FlatBufferEnum.class).withArtifactReference(reference);

        Schema schema = processor.buildSchema();
        schema.setNamespace(schemaNamespace);
        schema.setName(schemaName);
        schema.setFileExtension(fileExtension);
        schema.setFileIdentifier(fileIdentifier);
        schema.setIncludes(schemaIncludes);

        if (validateSchema) {
            Validator validator = new Validator().withSchema(schema);
            validator.validateSchema();
            schema.setValidSchema(validator.getErrors().isEmpty());
            Application.getLogger().info(validator.getErrorComments());
        }

        if (outputDirectory == null) {
            log.debug("outputDirectory is NULL");
        } else {
            log.debug("outputDirectory is: " + outputDirectory);
        }

        FileGeneration fg = new FileGeneration(outputDirectory);
        try {
            fg.writeSchema(schema);
        } catch (IOException e) {
            log.error("Unable to write schemas to disk", e);
        }
    }

    /**
     * @return the application logger defaulting to {@link}SystemStreamLog
     */
    public static Log getLogger() {
        if (log == null)
            log = new SystemStreamLog();
        return log;
    }
}
