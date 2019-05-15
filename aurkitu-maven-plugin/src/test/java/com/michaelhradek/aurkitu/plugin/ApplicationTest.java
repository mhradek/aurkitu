package com.michaelhradek.aurkitu.plugin;

import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author m.hradek
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationTest extends AbstractMojoTestCase {

    @Mock
    private MavenProject mockProject;

    @Override
    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWrite() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Schema schema = new Schema();
        schema.setName("test-schema-name-Application-Write");
        List<Schema> processedSchemas = new ArrayList<>();
        processedSchemas.add(schema);

        Application application = new Application();

        // Get the private method
        Method writeMethod = getPrivateApplicationMethod(application, "write");

        // Fill application with required values
        Field outputDirectoryField = application.getClass().getDeclaredField("outputDirectory");
        outputDirectoryField.setAccessible(true);
        outputDirectoryField.set(application, new File("./unit-test"));

        writeMethod.invoke(application, processedSchemas);
    }

    @Test
    public void testParse() throws IllegalAccessException, InvocationTargetException {
        Schema schema = new Schema();
        schema.setName("test-schema-name-Application-Parse");
        List<Schema> processedSchemas = new ArrayList<>();
        processedSchemas.add(schema);

        Application application = new Application();

        // Get the private method
        Method parseMethod = getPrivateApplicationMethod(application, "parse");

        ArtifactReference reference = new ArtifactReference(null, null, null, null, null);

        parseMethod.invoke(application, processedSchemas, reference);
    }

    @Test
    public void testSetup() throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Application application = new Application();

        // Get the private method
        Method setupMethod = getPrivateApplicationMethod(application, "setup");

        Mockito.when(mockProject.getDependencyArtifacts()).thenReturn(new HashSet<>());
        ArtifactReference reference = new ArtifactReference(mockProject, null, null, null, null);

        // Fill application with required values
        Field generateVersionField = application.getClass().getDeclaredField("generateVersion");
        generateVersionField.setAccessible(true);
        generateVersionField.set(application, false);

        setupMethod.invoke(application, reference);
    }

    @Test
    public void testLog() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Application application = new Application();

        // Get the private method
        Method logMethod = getPrivateApplicationMethod(application, "log");

        // Fill application with required values
        Field outputDirectoryField = application.getClass().getDeclaredField("outputDirectory");
        outputDirectoryField.setAccessible(true);
        outputDirectoryField.set(application, new File("./unit-test"));

        logMethod.invoke(application);
    }

    @Test
    public void testExecute() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        Application application = new Application();

        // Get the private method
        Method executeMethod = getPrivateApplicationMethod(application, "execute");

        // Fill application with required values
        Field outputDirectoryField = application.getClass().getDeclaredField("outputDirectory");
        outputDirectoryField.setAccessible(true);
        outputDirectoryField.set(application, new File("./unit-test"));

        Field generateVersionField = application.getClass().getDeclaredField("generateVersion");
        generateVersionField.setAccessible(true);
        generateVersionField.set(application, false);

        Mockito.when(mockProject.getDependencyArtifacts()).thenReturn(new HashSet<>());
        Field mavenProjectField = application.getClass().getDeclaredField("project");
        mavenProjectField.setAccessible(true);
        mavenProjectField.set(application, mockProject);

        Field useSchemaCachingField = application.getClass().getDeclaredField("useSchemaCaching");
        useSchemaCachingField.setAccessible(true);
        useSchemaCachingField.set(application, false);

        executeMethod.invoke(application);
    }

    /**
     * @param application
     * @param methodName
     * @return
     */
    private static Method getPrivateApplicationMethod(Application application, String methodName) {

        Method targetMethod = null;
        for (Method method : application.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                targetMethod = method;
                break;
            }
        }

        // Makes sure it was found and that it is accessible
        Assert.assertNotNull(targetMethod);
        targetMethod.setAccessible(true);

        return targetMethod;
    }

    /**
     * The commented block only works when the POM is set to run with some special settings. Will probably need to move this
     * into an integration test block that runs special POM files/projects to validate the POM files.
     *
     * @throws Exception
     */
    @Test
    public void testBasicRead() {
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
}
