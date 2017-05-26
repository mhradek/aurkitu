/**
 * 
 */
package com.michaelhradek.aurkitu;

import java.io.File;
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

  }
}
