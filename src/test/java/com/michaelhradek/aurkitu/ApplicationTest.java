/**
 *
 */
package com.michaelhradek.aurkitu;

import java.io.File;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author m.hradek
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
    public MojoRule rule = new MojoRule() {

        @Override
        protected void before() throws Throwable {}

        @Override
        protected void after() {}
    };

    /**
     * @throws Exception Unable to locate file.
     */
    @Test
    public void testBasicRead() throws Exception {
        File testPom = new File(getBasedir(),"src/test/resources/plugin-basic/pom.xml");
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());
        Assert.assertTrue(testPom.isFile());

        Application mojo = new Application();
        mojo = (Application) this.configureMojo(
                mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
        );

        assertNotNull(mojo);
        mojo.execute();
    }

//    @Test(expected = MojoExecutionException.class)
//    public void testBasicReadWithProject() throws Exception {
//        File testPom = new File(getBasedir(),"src/test/resources/plugin-basic-with-project/pom.xml");
//        Assert.assertNotNull(testPom);        Assert.assertTrue(testPom.exists());
//        Assert.assertTrue(testPom.isFile());
//
//        Application mojo = new Application();
//        mojo = (Application) this.configureMojo(
//                mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
//        );
//
//        assertNotNull(mojo);
//        mojo.execute();
//    }
}
