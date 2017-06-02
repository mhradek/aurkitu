/**
 * 
 */
package com.michaelhradek.aurkitu;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.FileGeneration;
import com.michaelhradek.aurkitu.core.Processor;
import com.michaelhradek.aurkitu.core.output.Schema;

/**
 * @author m.hradek
 * @date May 18, 2017
 * 
 */
@Mojo(name = Application.MOJO_NAME, defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Application extends AbstractMojo {

  public static final String MOJO_NAME = "create-flatbuffer-schema";

  @Parameter(property = Application.MOJO_NAME + ".ouput-dir",
      defaultValue = "${project.build.directory}/aurkitu/schemas")
  private File outputDirectory;

  @Parameter(property = Application.MOJO_NAME + ".search-path", defaultValue = ".*")
  private String searchPath;

  @Parameter(property = Application.MOJO_NAME + ".schema-namespace",
      defaultValue = "generated.flatbuffers")
  private String namespace;

  @Parameter(property = Application.MOJO_NAME + ".schema-name")
  private String schemaName;

  public void execute() throws MojoExecutionException, MojoFailureException {
    getLog().info("execute: " + Application.MOJO_NAME);

    Processor processor =
        new Processor().withSource(FlatBufferTable.class).withSource(FlatBufferEnum.class);

    Schema schema = processor.buildSchema();
    schema.setNamespace(namespace);
    schema.setName(schemaName);

    FileGeneration fg = new FileGeneration(outputDirectory);
    try {
      fg.writeSchema(schema);
    } catch (IOException e) {
      getLog().error("Unable to write schemas to disk", e);
    }
  }
}
