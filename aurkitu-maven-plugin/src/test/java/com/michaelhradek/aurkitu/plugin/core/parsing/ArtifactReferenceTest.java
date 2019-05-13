package com.michaelhradek.aurkitu.plugin.core.parsing;

import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.junit.Test;

public class ArtifactReferenceTest {

    @Test
    public void testArtifactReference() {
        ArtifactReference reference =
                new ArtifactReference(null, null, null, null, null);

        Assert.assertNotNull(reference);
        Assert.assertNull(reference.getMavenProject());
        Assert.assertNull(reference.getRepoSession());
        Assert.assertNull(reference.getRepositories());
        Assert.assertNull(reference.getRepoSystem());
        Assert.assertNull((reference.getSpecifiedDependencies()));
    }

    @Test
    public void testEqualsAndHashCode() {
        ArtifactReference referenceOne =
                new ArtifactReference(null, null, null, null, null);
        ArtifactReference referenceTwo =
                new ArtifactReference(null, null, null, null, null);
        Assert.assertEquals(referenceOne, referenceTwo);
        Assert.assertEquals(referenceOne.hashCode(), referenceTwo.hashCode());
        Assert.assertTrue(referenceOne.equals(referenceTwo));
        Assert.assertTrue(referenceTwo.equals(referenceOne));

        MavenProject mavenProject = new MavenProject();
        referenceTwo = new ArtifactReference(mavenProject, null, null, null, null);

        Assert.assertNotEquals(referenceOne, referenceTwo);
        Assert.assertNotEquals(referenceOne.hashCode(), referenceTwo.hashCode());
        Assert.assertFalse(referenceOne.equals(referenceTwo));
        Assert.assertFalse(referenceTwo.equals(referenceOne));
    }
}
