package com.michaelhradek.aurkitu.plugin.core.parser;

import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
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
}
