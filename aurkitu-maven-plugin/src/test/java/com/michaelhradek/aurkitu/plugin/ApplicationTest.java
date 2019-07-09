package com.michaelhradek.aurkitu.plugin;

import com.michaelhradek.aurkitu.plugin.core.Utilities;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.output.components.Namespace;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathSearchType;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * @author m.hradek
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Utilities.class, Namespace.class})
public class ApplicationTest extends AbstractMojoTestCase {

    private static final String DIR_UNIT_TEST = "./target/unit-test";

    @Mock
    private MavenProject mockProject;

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

    @Override
    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWrite() {

        try {
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
            outputDirectoryField.set(application, new File(DIR_UNIT_TEST));

            writeMethod.invoke(application, processedSchemas);

            outputDirectoryField.set(application, null);
            writeMethod.invoke(application, processedSchemas);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            Assert.fail("Unable to write files via Application::write " + e.getMessage());
        } catch (InvocationTargetException ite) {
            if (ite.getCause() instanceof IOException) {
                // Good, we expected this...
                return;
            }

            Assert.fail("An unknown InvocationTargetException occurred");
        }
    }

    @Test
    public void testParse() {
        try {
            Schema schema = new Schema();
            schema.setName("test-schema-name-Application-Parse");
            List<Schema> processedSchemas = new ArrayList<>();
            processedSchemas.add(schema);

            Application application = new Application();

            // Get the private method
            Method parseMethod = getPrivateApplicationMethod(application, "parse");

            ArtifactReference reference = new ArtifactReference(null, null, null, null, null);

            parseMethod.invoke(application, processedSchemas, reference);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Assert.fail("Unable to parse schemas via Application::parse " + e.getMessage());
        }
    }

    @Test
    public void testSetup() throws DependencyResolutionRequiredException, MalformedURLException, ArtifactResolutionException {
        try {
            Application application = new Application();

            // Get the private method
            Method setupMethod = getPrivateApplicationMethod(application, "setup");

            List<String> compileClasspathElements = new ArrayList<>();
            compileClasspathElements.add("./aurkitu-test-service/target/classes");

            Mockito.when(mockProject.getDependencyArtifacts()).thenReturn(new HashSet<>());
            Mockito.when(mockProject.getGroupId()).thenReturn("test.group.id");
            Mockito.when(mockProject.getArtifactId()).thenReturn("test.artifact.id");
            Mockito.when(mockProject.getCompileClasspathElements()).thenReturn(compileClasspathElements);
            ArtifactReference reference = new ArtifactReference(mockProject, null, null, null, null);

            // Fill application with required values
            Field generateVersionField = application.getClass().getDeclaredField("generateVersion");
            generateVersionField.setAccessible(true);
            generateVersionField.set(application, false);

            Field consolidatedSchemasField = application.getClass().getDeclaredField("consolidatedSchemas");
            consolidatedSchemasField.setAccessible(true);
            consolidatedSchemasField.set(application, false);
            setupMethod.invoke(application, reference);

            PowerMockito.mockStatic(Utilities.class);
            List<ClasspathReference> classpathReferenceList = new ArrayList<>();
            ClasspathReference classpathReference = new ClasspathReference(new URL("jar:file://some/test/file.jar!/"), "some-test-group-id", "some-test-artifact-id");
            classpathReferenceList.add(classpathReference);

            Mockito.when(Utilities.buildProjectClasspathList(Mockito.any(ArtifactReference.class), Mockito.any(ClasspathSearchType.class))).thenReturn(classpathReferenceList);
            setupMethod.invoke(application, reference);

            PowerMockito.mockStatic(Namespace.class);
            Mockito.when(Namespace.parse(Mockito.anyString())).thenReturn(null);
            setupMethod.invoke(application, reference);

            Mockito.when(Namespace.parse(Mockito.anyString())).thenReturn(new Namespace("", "", ""));
            setupMethod.invoke(application, reference);

            Mockito.when(Namespace.parse(Mockito.anyString())).thenReturn(new Namespace("", "SOME_IDENTIFIER", ""));
            setupMethod.invoke(application, reference);

        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            Assert.fail("Unable to setup schemas via Application::setup " + e.getMessage());
        }
    }

    @Test
    public void testLog() {
        try {
            Application application = new Application();

            // Get the private method
            Method logMethod = getPrivateApplicationMethod(application, "log");

            // Fill application with required values
            Field outputDirectoryField = application.getClass().getDeclaredField("outputDirectory");
            outputDirectoryField.setAccessible(true);
            outputDirectoryField.set(application, new File(DIR_UNIT_TEST));

            logMethod.invoke(application);

            Field namespaceOverrideMapField = application.getClass().getDeclaredField("namespaceOverrideMap");
            namespaceOverrideMapField.setAccessible(true);
            namespaceOverrideMapField.set(application, null);

            logMethod.invoke(application);

            Map<String, String> namespaceOverrideMap = new HashMap<>();
            namespaceOverrideMap.put("someMapKey", "someMapValue");

            namespaceOverrideMapField.set(application, namespaceOverrideMap);

            logMethod.invoke(application);

            Field schemaIncludesField = application.getClass().getDeclaredField("schemaIncludes");
            schemaIncludesField.setAccessible(true);
            schemaIncludesField.set(application, null);

            logMethod.invoke(application);

            Set<String> schemaIncludes = new HashSet<>();
            schemaIncludes.add("testSchemaIncludes");
            schemaIncludesField.set(application, schemaIncludes);

            logMethod.invoke(application);

            Field specifiedDependenciesField = application.getClass().getDeclaredField("specifiedDependencies");
            specifiedDependenciesField.setAccessible(true);
            specifiedDependenciesField.set(application, null);

            logMethod.invoke(application);

            List<String> specifiedDependencies = new ArrayList<>();
            specifiedDependencies.add("testSpecifiedDependencies");
            specifiedDependenciesField.set(application, specifiedDependencies);

            logMethod.invoke(application);

        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            Assert.fail("Unable to run the initiate setting log via Application::log " + e.getMessage());
        }
    }

    @Test
    public void testExecute() {
        try {
            Application application = new Application();

            // Get the private method
            Method executeMethod = getPrivateApplicationMethod(application, "execute");

            // Fill application with required values
            Field outputDirectoryField = application.getClass().getDeclaredField("outputDirectory");
            outputDirectoryField.setAccessible(true);
            outputDirectoryField.set(application, new File(DIR_UNIT_TEST));

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

            useSchemaCachingField.set(application, true);

            executeMethod.invoke(application);

            PowerMockito.mockStatic(Utilities.class);
            Mockito.when(Utilities.areSchemasPresent(Mockito.anyListOf(Schema.class), Mockito.anyObject())).thenReturn(true);

            executeMethod.invoke(application);

        } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            Assert.fail("Unable to execute plugin via Application::execute " + e.getMessage());
        }
    }

    @Test
    public void testBasicRead() {
        File testPom = new File(getBasedir(), "src/test/resources/plugin-basic/pom.xml");
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());
        Assert.assertTrue(testPom.isFile());
    }
}
