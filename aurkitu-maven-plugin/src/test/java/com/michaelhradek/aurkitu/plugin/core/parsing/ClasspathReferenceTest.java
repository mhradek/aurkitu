package com.michaelhradek.aurkitu.plugin.core.parsing;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class ClasspathReferenceTest {

    private static String TEST_ARTIFACT = "come.artifact.name";
    private static String TEST_GROUP = "group_id";

    @Test
    public void testClasspathReference() throws MalformedURLException {
        ClasspathReference classpathReference = new ClasspathReference(null, null, null);
        Assert.assertNull(classpathReference.getUrl());
        Assert.assertNull(classpathReference.getArtifactId());
        Assert.assertNull(classpathReference.getGroupId());
        Assert.assertNotNull(classpathReference.getNamespace());

        ClasspathReference classpathReferenceTwo = new ClasspathReference(null, null, null);
        Assert.assertTrue(classpathReference.equals(classpathReferenceTwo));
        Assert.assertTrue(classpathReferenceTwo.equals(classpathReference));
        Assert.assertEquals(classpathReference.hashCode(), classpathReferenceTwo.hashCode());

        final URL TEST_URL = new URL("file:/some/url/to/some/jar");

        classpathReference = new ClasspathReference(TEST_URL, null, TEST_ARTIFACT);
        Assert.assertEquals(TEST_ARTIFACT, classpathReference.getArtifactId());
        Assert.assertEquals(null, classpathReference.getGroupId());
        Assert.assertNotNull(classpathReference.getNamespace());

        classpathReference = new ClasspathReference(TEST_URL, TEST_GROUP, null);
        Assert.assertEquals(null, classpathReference.getArtifactId());
        Assert.assertEquals(TEST_GROUP, classpathReference.getGroupId());
        Assert.assertNotNull(classpathReference.getNamespace());

        classpathReference = new ClasspathReference(TEST_URL, TEST_GROUP, TEST_ARTIFACT);
        Assert.assertEquals(TEST_URL, classpathReference.getUrl());
        Assert.assertEquals(TEST_ARTIFACT, classpathReference.getArtifactId());
        Assert.assertEquals(TEST_GROUP, classpathReference.getGroupId());

        final String testNamespace = TEST_GROUP + "." + TEST_ARTIFACT;
        Assert.assertEquals(testNamespace, classpathReference.getNamespace().toString());

        Assert.assertFalse(classpathReference.equals(classpathReferenceTwo));
        Assert.assertFalse(classpathReferenceTwo.equals(classpathReference));
        Assert.assertNotEquals(classpathReference.hashCode(), classpathReferenceTwo.hashCode());
    }
}
