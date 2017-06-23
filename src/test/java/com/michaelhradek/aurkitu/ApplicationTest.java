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
  public void testBasic() throws Exception {
    File testPom = new File(getBasedir(), "src/test/resources/plugin-basic/pom.xml");
    Assert.assertEquals(true, testPom.exists());
    Assert.assertEquals(true, testPom.isFile());

    Application mojo = (Application) lookupMojo(Application.MOJO_GOAL, testPom);
    Assert.assertNotNull(mojo);
    mojo.execute();
  }

  @Test
  public void testNoConfig() throws Exception {
    /**
     * DISABLED Need to work on getting defaults to work within the test harness.
     * https://stackoverflow.com/questions/31528763/how-to-populate-parameter-defaultvalue-in-maven-abstractmojotestcase/36064396
     * 
     * @Rule public MojoRule mojoRule = new MojoRule();
     * 
     * @Test public void noSource() throws Exception { MyPlugin plugin = (MyPlugin)
     *       mojoRule.lookupConfiguredMojo(loadPom("testpom1"), "myGoal"); plugin.execute();
     * 
     *       assertThat(plugin.getSomeInformation()).isEmpty(); }
     * 
     *       public File loadPom(String folderName) { return new File("src/test/resources/",
     *       folderName); }
     * 
     *
     *       File testPom = new File(getBasedir(), "src/test/resources/plugin-no-config/pom.xml");
     *       Assert.assertEquals(true, testPom.exists()); Assert.assertEquals(true,
     *       testPom.isFile());
     * 
     *       Application mojo = (Application) lookupMojo(Application.MOJO_GOAL, testPom);
     *       Assert.assertNotNull(mojo); mojo.execute(); /
     **/
  }
}
