/**
 * 
 */
package com.michaelhradek.aurkitu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author m.hradek
 * @date May 18, 2017
 * 
 */
@Mojo(name = "FlatBuffer", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Application extends AbstractMojo {

  private Logger logger = Config.getLogger(getClass());

  @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
  private File outputDirectory;

  @Parameter(defaultValue = ".*", property = "searchPath", required = true)
  private String searchPath;

  public void execute() throws MojoExecutionException, MojoFailureException {

    File file = outputDirectory;

    if (!file.exists()) {
      logger.log(Level.FINE, "File does not exist; creating directories");
      file.mkdirs();
    }

    File touch = new File(file, "schema.fbs");

    FileWriter writer = null;
    try {
      writer = new FileWriter(touch);

      writer.write("schema.fbs");
    } catch (IOException e) {
      throw new MojoExecutionException("Error creating file: " + touch, e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          logger.log(Level.FINE, "Unable to close writer. " + e.getMessage());
        }
      }
    }

  }
}
