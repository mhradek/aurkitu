package com.michaelhradek.aurkitu;

import com.michaelhradek.aurkitu.plugin.Application;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;

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

    @Test
    public void testBasicWithProjectRead() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/plugin-basic-with-project/pom.xml");
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());
        Assert.assertTrue(testPom.isFile());

        Application mojo = new Application();
        mojo = (Application) this.configureMojo(
            mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
        );

        Field projectField = mojo.getClass().getDeclaredField("project");
        projectField.setAccessible(true);
        MavenProject mavenProject = (MavenProject) projectField.get(mojo);

        Assert.assertNotNull(mavenProject);
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
