package com.michaelhradek.aurkitu;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;

/**
 * @author m.hradek
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationTest extends AbstractMojoTestCase {

    @Mock
    private MavenProject mockProject;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Rule
    public MojoRule rule = new MojoRule() {

        @Override
        protected void before() {
            // Empty
        }

        @Override
        protected void after() {
            // Empty
        }
    };

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * @throws Exception Unable to locate file.
     */
    @Test
    public void testBasicRead() throws Exception {
        Mockito.when(mockProject.getCompileClasspathElements()).thenReturn(new ArrayList<>());

        File testPom = new File(getBasedir(),"src/test/resources/plugin-basic/pom.xml");
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());
        Assert.assertTrue(testPom.isFile());

//        Application mojo = new Application();
//        mojo = (Application) this.configureMojo(
//                mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
//        );
//
//        assertNotNull(mojo);
//        mojo.execute();
    }

    @Test
    public void testBasicWithProjectRead() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/plugin-basic-with-project/pom.xml");
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());
        Assert.assertTrue(testPom.isFile());

//        Application mojo = new Application();
//        mojo = (Application) this.configureMojo(
//            mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
//        );
//
//        Field projectField = mojo.getClass().getDeclaredField("project");
//        projectField.setAccessible(true);
//        MavenProject mavenProject = (MavenProject) projectField.get(mojo);
//
//        Assert.assertNotNull(mavenProject);
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
