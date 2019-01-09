package com.michaelhradek.aurkitu.plugin.core.parser;

import com.michaelhradek.aurkitu.plugin.core.parsing.ArtifactReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ArtifactReferenceTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {

    }

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
