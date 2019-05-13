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
//
//        // get the project variable
//        MavenProject project = (MavenProject) this.getVariableValueFromObject(mojo, "project");
//
//        // get the groupId from inside the project
//        final String groupId = (String) this.getVariableValueFromObject(project, "groupId");
//        final String artifactId = (String) this.getVariableValueFromObject(project, "artifactId");
//
//        // assert
//        Assert.assertEquals("com.michaelhradek.aurkitu.test", groupId);
//        Assert.assertEquals("plugin-basic", artifactId);
//
//        // get values from configuration
//        Field field = mojo.getClass().getDeclaredField("outputDirectory");
//        field.setAccessible(true);
//        Assert.assertEquals("target/test-dir", field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("specifiedDependencies");
//        field.setAccessible(true);
//        Assert.assertEquals(null, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("schemaNamespace");
//        field.setAccessible(true);
//        Assert.assertEquals(null, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("schemaIncludes");
//        field.setAccessible(true);
//        Assert.assertEquals(null, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("validateSchema");
//        field.setAccessible(true);
//        Assert.assertEquals(true, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("schemaName");
//        field.setAccessible(true);
//        Assert.assertEquals("test-schema", field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("schemaFileIdentifier");
//        field.setAccessible(true);
//        Assert.assertEquals(null, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("fileExtension");
//        field.setAccessible(true);
//        Assert.assertEquals(null, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("namespaceOverrideMap");
//        field.setAccessible(true);
//        Assert.assertNotNull(field.get(mojo));
//        Map<String, String> namespaceOverrideMap = (Map<String, String>) field.get(mojo);
//        Assert.assertEquals(1, namespaceOverrideMap.size());
//        Assert.assertTrue(namespaceOverrideMap.containsKey("search.namespace"));
//        Assert.assertTrue(namespaceOverrideMap.containsValue("replacement.namespace"));
//        Assert.assertEquals("replacement.namespace", namespaceOverrideMap.get("search.namespace"));
//
//        field = mojo.getClass().getDeclaredField("generateVersion");
//        field.setAccessible(true);
//        Assert.assertEquals(true, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("consolidatedSchemas");
//        field.setAccessible(true);
//        Assert.assertEquals(true, field.get(mojo));
//
//        field = mojo.getClass().getDeclaredField("useSchemaCaching");
//        field.setAccessible(true);
//        Assert.assertEquals(false, field.get(mojo));
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
