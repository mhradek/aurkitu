/**
 * 
 */
package com.michaelhradek.aurkitu;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
public class ApplicationTest extends AbstractMojoTestCase {
  /**
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    // required for mojo lookups to work
    super.setUp();
  }

  @Rule
  public MojoRule mojoRule = new MojoRule();

  /**
   * @throws Exception
   */
  @Test
  public void testBasicRead() throws Exception {
    File testPom = new File(getBasedir(), "src/test/resources/plugin-basic/pom.xml");
    Assert.assertEquals(true, testPom.exists());
    Assert.assertEquals(true, testPom.isFile());

    Application mojo = (Application) lookupMojo(Application.MOJO_GOAL, testPom);
    Assert.assertNotNull(mojo);
  }

  @Test
  public void testBasicConfig() throws Exception {
    // Application mojo =
    // (Application) mojoRule.lookupConfiguredMojo(loadPom("plugin-basic"), Application.MOJO_GOAL);
    // mojo.execute();
  }

  public File loadPom(String folderName) {
    return new File("src/test/resources/", folderName);
  }
}
