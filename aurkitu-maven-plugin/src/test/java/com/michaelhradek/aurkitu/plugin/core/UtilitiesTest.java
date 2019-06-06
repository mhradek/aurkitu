package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.Application;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import com.michaelhradek.aurkitu.plugin.core.parsing.ClasspathReference;
import com.michaelhradek.aurkitu.plugin.test.SampleClassTable;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UtilitiesTest extends AbstractMojoTestCase {

    private static String OUTPUT_DIRECTORY = "target/aurkitu/utilities/test";
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

    @Override
    protected void setUp() throws Exception {

        // required for mojo lookups to work
        super.setUp();
    }

    @Test()
    public void testPrivateConstructor() throws NoSuchMethodException {
        final Constructor<Utilities> constructor = Utilities.class.getDeclaredConstructor();
        Assert.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    public void testIsLowerCaseType() {

        Double primDouble = 123D;
        Assert.assertTrue(Utilities.isLowerCaseType(primDouble.getClass()));

        Float primFloat = 123F;
        Assert.assertTrue(Utilities.isLowerCaseType(primFloat.getClass()));

        Long primLong = 123L;
        Assert.assertTrue(Utilities.isLowerCaseType(primLong.getClass()));

        Integer primInteger = 123;
        Assert.assertTrue(Utilities.isLowerCaseType(primInteger.getClass()));

        Short primShort = 12;
        Assert.assertTrue(Utilities.isLowerCaseType(primShort.getClass()));

        Character primCharacter = 'd';
        Assert.assertTrue(Utilities.isLowerCaseType(primCharacter.getClass()));

        Byte primByte = 8;
        Assert.assertTrue(Utilities.isLowerCaseType(primByte.getClass()));

        Boolean primBoolean = true;
        Assert.assertTrue(Utilities.isLowerCaseType(primBoolean.getClass()));

        String primString = "Test string";
        Assert.assertTrue(Utilities.isLowerCaseType(primString.getClass()));

        Schema schema = new Schema();
        Assert.assertFalse(Utilities.isLowerCaseType(schema.getClass()));

        Assert.assertFalse(Utilities.isLowerCaseType(Application.class));

        Assert.assertFalse(Utilities.isLowerCaseType(void.class));
        Assert.assertFalse(Utilities.isLowerCaseType(Void.class));
    }

    @Test
    public void testExecuteActionOnSpecifiedClassLoader() {
        Class<?> result = Utilities.executeActionOnSpecifiedClassLoader(URLClassLoader.getSystemClassLoader(),
                new Utilities.ExecutableAction<Class<?>>() {

                    public Class<?> run() {
                        try {
                            return Class.forName(SampleClassTable.class.getName());
                        } catch (ClassNotFoundException e) {
                            return null;
                        }
                    }
                });

        Assert.assertNotNull(result);
        Assert.assertEquals(SampleClassTable.class, result);
    }

    @Test
    public void testBuildProjectClasspathList() throws Exception {
        // TODO
    }

    @Test
    public void testBuildReflections() {
        // TODO
    }

    @Test
    public void testIsSchemaPresent() throws Exception {

        // File will not exist
        Assert.assertFalse(Utilities.isSchemaPresent(new Schema(), new File("/")));

        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());

        processor.execute();
        Schema schema = processor.getProcessedSchemas().get(0);
        FileGeneration gen = new FileGeneration(new File(OUTPUT_DIRECTORY));
        gen.writeSchema(schema);

        // No name set so even with caching it will return false because the filename keeps changing
        Assert.assertFalse(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));

        schema.setName("some-test-name");
        gen.writeSchema(schema);

        // File name is consistent and thus we can check cached file
        Assert.assertTrue(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));

        // The directory is not present
        gen = new FileGeneration(new File(OUTPUT_DIRECTORY + "/missing"));
        Assert.assertFalse(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));

        gen = new FileGeneration(new File(OUTPUT_DIRECTORY));
        schema.setName("");
        Assert.assertFalse(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));
        schema.setName(null);
        Assert.assertFalse(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));
    }

    @Test
    public void testAreSchemasPresent() throws Exception {

        List<Schema> schemas = new ArrayList<>();
        Schema schemaA = new Schema();
        schemaA.setName("schemaA");
        schemas.add(schemaA);

        Assert.assertFalse(Utilities.areSchemasPresent(schemas, new File("/")));

        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());

        processor.execute();
        Schema schemaB = processor.getProcessedSchemas().get(0);
        schemaB.setName("schemaB");
        FileGeneration gen = new FileGeneration(new File(OUTPUT_DIRECTORY));
        gen.writeSchema(schemaB);

        schemas.add(schemaB);
        Assert.assertFalse(Utilities.areSchemasPresent(schemas, gen.getOutputDirectory()));

        // A was never present so removing it makes it true since B exists
        schemas.remove(schemaA);
        Assert.assertTrue(Utilities.areSchemasPresent(schemas, gen.getOutputDirectory()));
    }

    @Test
    public void testIsResolutionRequired() {

        ArtifactReference artifactReference = new ArtifactReference(null, null, null, null, null);

        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.some-group", null), artifactReference));
        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.some-group", "blah-blah"), artifactReference));

        List<String> specifiedDependencies = new ArrayList<>();
        specifiedDependencies.add("bork_kasjf:j2;3jr");
        specifiedDependencies.add("com.somecompany.team");
        specifiedDependencies.add("com.othercompany.clan:the-project");
        specifiedDependencies.add("org.ngo.subversive:yet-another-project:0.0.3");

        artifactReference = new ArtifactReference(null, null, null, null, specifiedDependencies);

        Assert.assertFalse(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.some-group", null), artifactReference));
        Assert.assertFalse(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.some-group", "blah-blah"), artifactReference));

        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("com.somecompany.team", "blah-blah"), artifactReference));
        Assert.assertFalse(Utilities.isArtifactResolutionRequired(getFauxArtifact("com.othercompany.clan", "blah-blah"), artifactReference));

        Assert.assertFalse(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.ngo.subversive", "blarg"), artifactReference));
        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.ngo.subversive", "yet-another-project"), artifactReference));

        // Skip the check because the specified dependencies list was empty or null
        artifactReference = new ArtifactReference(null, null, null, null, null);
        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("com.somecompany.team", "blah-blah"), artifactReference));
        specifiedDependencies = new ArrayList<>();
        artifactReference = new ArtifactReference(null, null, null, null, specifiedDependencies);
        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("com.somecompany.team", "blah-blah"), artifactReference));
    }

    @Test
    public void testGetCurrentProject() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/plugin-basic-with-project/pom.xml");
        Application mojo = new Application();
        mojo = (Application) this.configureMojo(
                mojo, extractPluginConfiguration(Application.MOJO_NAME, testPom)
        );

        Field projectField = mojo.getClass().getDeclaredField("project");
        projectField.setAccessible(true);
        MavenProject mavenProject = (MavenProject) projectField.get(mojo);

        final String projectName = Utilities.getCurrentProject(new ArtifactReference(mavenProject, null, null, null, null));
        Assert.assertEquals("com.michaelhradek.aurkitu.test:plugin-basic", projectName);
    }

    @Test
    public void testArrayForClasspathReferenceList() throws MalformedURLException {
        URL[] result = Utilities.arrayForClasspathReferenceList(null);
        Assert.assertNull(result);

        result = Utilities.arrayForClasspathReferenceList(new ArrayList<>());
        Assert.assertNotNull(result);

        Assert.assertEquals(0, result.length);

        final int LIMIT = 10;
        List<ClasspathReference> list = new ArrayList<>();
        for (int i = 0; i < LIMIT; i++) {
            list.add(new ClasspathReference(new URL("file:/" + UUID.randomUUID().toString() + "/"), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        }

        result = Utilities.arrayForClasspathReferenceList(list);
        Assert.assertEquals(LIMIT, result.length);
    }

    @Test
    public void testExtractDependencyDetails() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        final String testDependencyGroupIdOnly = "com.test.package";
        final String testDependencyGroupIdAndArtifactId = "com.test.package-group:test-artifact";

        // Get an instance of the private constructor Utilities class.
        Constructor<Utilities> constructor = Utilities.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Utilities utilities = constructor.newInstance();

        // Get the private method that we are testing
        Method method = utilities.getClass().getDeclaredMethod("extractDependencyDetails", String.class);
        method.setAccessible(true);

        // This is a private, static inner class used nowhere else but inside the parser
        Object details = method.invoke(utilities, testDependencyGroupIdOnly);

        // Get the details from the fields
        Field specifiedGroupIdField = details.getClass().getDeclaredField("specifiedGroupId");
        specifiedGroupIdField.setAccessible(true);
        String specifiedGroupId = (String) specifiedGroupIdField.get(details);

        Field specifiedArtifactIdField = details.getClass().getDeclaredField("specifiedArtifactId");
        specifiedArtifactIdField.setAccessible(true);
        String specifiedArtifactId = (String) specifiedArtifactIdField.get(details);

        // Test "package" only scenario
        Assert.assertEquals(testDependencyGroupIdOnly, specifiedGroupId);
        Assert.assertEquals(null, specifiedArtifactId);

        details = method.invoke(utilities, testDependencyGroupIdAndArtifactId);

        // Get the details from the fields
        specifiedGroupIdField = details.getClass().getDeclaredField("specifiedGroupId");
        specifiedGroupIdField.setAccessible(true);
        specifiedGroupId = (String) specifiedGroupIdField.get(details);

        specifiedArtifactIdField = details.getClass().getDeclaredField("specifiedArtifactId");
        specifiedArtifactIdField.setAccessible(true);
        specifiedArtifactId = (String) specifiedArtifactIdField.get(details);

        // Test "package" and "artifact" scenario
        Assert.assertEquals("com.test.package-group", specifiedGroupId);
        Assert.assertEquals("test-artifact", specifiedArtifactId);
    }

    /**
     * @param groupId    test group id
     * @param artifactId test artifact id
     * @return the faux artifact
     */
    private Artifact getFauxArtifact(final String groupId, final String artifactId) {

        return new Artifact() {

            public String getGroupId() {
                return groupId;
            }

            public void setGroupId(String s) {
                // Empty
            }

            public String getArtifactId() {
                return artifactId;
            }

            public void setArtifactId(String s) {
                // Empty
            }

            public String getVersion() {
                return null;
            }

            public void setVersion(String s) {
                // Empty
            }

            public String getScope() {
                return null;
            }

            public void setScope(String s) {
                // Empty
            }

            public String getType() {
                return null;
            }

            public String getClassifier() {
                return null;
            }

            public boolean hasClassifier() {
                return false;
            }

            public File getFile() {
                return null;
            }

            public void setFile(File file) {
                // Empty
            }

            public String getBaseVersion() {
                return null;
            }

            public void setBaseVersion(String s) {
                // Empty
            }

            public String getId() {
                return null;
            }

            public String getDependencyConflictId() {
                return null;
            }

            public void addMetadata(ArtifactMetadata artifactMetadata) {
                // Empty
            }

            public Collection<ArtifactMetadata> getMetadataList() {
                return null;
            }

            public ArtifactRepository getRepository() {
                return null;
            }

            public void setRepository(ArtifactRepository artifactRepository) {
                // Empty
            }

            public void updateVersion(String s, ArtifactRepository artifactRepository) {
                // Empty
            }

            public String getDownloadUrl() {
                return null;
            }

            public void setDownloadUrl(String s) {
                // Empty
            }

            public ArtifactFilter getDependencyFilter() {
                return null;
            }

            public void setDependencyFilter(ArtifactFilter artifactFilter) {
                // Empty
            }

            public ArtifactHandler getArtifactHandler() {
                return null;
            }

            public void setArtifactHandler(ArtifactHandler artifactHandler) {
                // Empty
            }

            public List<String> getDependencyTrail() {
                return null;
            }

            public void setDependencyTrail(List<String> list) {
                // Empty
            }

            public VersionRange getVersionRange() {
                return null;
            }

            public void setVersionRange(VersionRange versionRange) {
                // Empty
            }

            public void selectVersion(String s) {
                // Empty
            }

            public boolean isSnapshot() {
                return false;
            }

            public boolean isResolved() {
                return false;
            }

            public void setResolved(boolean b) {
                // Empty
            }

            public void setResolvedVersion(String s) {
                // Empty
            }

            public boolean isRelease() {
                return false;
            }

            public void setRelease(boolean b) {
                // Empty
            }

            public List<ArtifactVersion> getAvailableVersions() {
                return null;
            }

            public void setAvailableVersions(List<ArtifactVersion> list) {
                // Empty
            }

            public boolean isOptional() {
                return false;
            }

            public void setOptional(boolean b) {
                // Empty
            }

            public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
                return null;
            }

            public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
                return false;
            }

            public int compareTo(Artifact o) {
                return 0;
            }
        };
    }
}

