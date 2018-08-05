package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.Schema;
import com.michaelhradek.aurkitu.test.SampleClassTable;
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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UtilitiesTest extends AbstractMojoTestCase {

    private static String OUTPUT_DIRECTORY = "target/aurkito/utilities/test";

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
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

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

    }

    @Test
    public void testBuildReflections() {

    }

    @Test
    public void testIsSchemaPresent() throws Exception {

        // File will not exist
        Assert.assertFalse(Utilities.isSchemaPresent(new Schema(), new File("/")));

        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class);

        Schema schema = processor.buildSchema();
        FileGeneration gen = new FileGeneration(new File(OUTPUT_DIRECTORY));
        gen.writeSchema(schema);

        // No name set so even with caching it will return false because the filename keeps changing
        Assert.assertFalse(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));

        schema.setName("some-test-name");
        gen.writeSchema(schema);

        // File name is consistent and thus we can check cached file
        Assert.assertTrue(Utilities.isSchemaPresent(schema, gen.getOutputDirectory()));
    }

    @Test
    public void testIsResolutionRequired() {

        ArtifactReference artifactReference = new ArtifactReference(null, null, null, null, null);

        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.some-group", null), artifactReference));
        Assert.assertTrue(Utilities.isArtifactResolutionRequired(getFauxArtifact("org.some-group", "blah-blah"), artifactReference));

        List<String> specifiedDependencies = new ArrayList<String>();
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

            public String getArtifactId() {
                return artifactId;
            }

            public String getVersion() {
                return null;
            }

            public void setVersion(String s) {

            }

            public String getScope() {
                return null;
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

            }

            public String getBaseVersion() {
                return null;
            }

            public void setBaseVersion(String s) {

            }

            public String getId() {
                return null;
            }

            public String getDependencyConflictId() {
                return null;
            }

            public void addMetadata(ArtifactMetadata artifactMetadata) {

            }

            public Collection<ArtifactMetadata> getMetadataList() {
                return null;
            }

            public void setRepository(ArtifactRepository artifactRepository) {

            }

            public ArtifactRepository getRepository() {
                return null;
            }

            public void updateVersion(String s, ArtifactRepository artifactRepository) {

            }

            public String getDownloadUrl() {
                return null;
            }

            public void setDownloadUrl(String s) {

            }

            public ArtifactFilter getDependencyFilter() {
                return null;
            }

            public void setDependencyFilter(ArtifactFilter artifactFilter) {

            }

            public ArtifactHandler getArtifactHandler() {
                return null;
            }

            public List<String> getDependencyTrail() {
                return null;
            }

            public void setDependencyTrail(List<String> list) {

            }

            public void setScope(String s) {

            }

            public VersionRange getVersionRange() {
                return null;
            }

            public void setVersionRange(VersionRange versionRange) {

            }

            public void selectVersion(String s) {

            }

            public void setGroupId(String s) {

            }

            public void setArtifactId(String s) {

            }

            public boolean isSnapshot() {
                return false;
            }

            public void setResolved(boolean b) {

            }

            public boolean isResolved() {
                return false;
            }

            public void setResolvedVersion(String s) {

            }

            public void setArtifactHandler(ArtifactHandler artifactHandler) {

            }

            public boolean isRelease() {
                return false;
            }

            public void setRelease(boolean b) {

            }

            public List<ArtifactVersion> getAvailableVersions() {
                return null;
            }

            public void setAvailableVersions(List<ArtifactVersion> list) {

            }

            public boolean isOptional() {
                return false;
            }

            public void setOptional(boolean b) {

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

