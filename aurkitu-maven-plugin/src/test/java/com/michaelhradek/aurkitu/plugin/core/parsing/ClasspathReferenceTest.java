package com.michaelhradek.aurkitu.plugin.core.parsing;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class ClasspathReferenceTest {

    private static String TEST_ARTIFACT = "come.artifact.name";
    private static String TEST_GROUP = "group-id";

    @Test
    public void testClasspathReference() throws MalformedURLException {
        ClasspathReference classpathReference = new ClasspathReference(null, null, null);
        Assert.assertNull(classpathReference.getUrl());
        Assert.assertNull(classpathReference.getArtifact());
        Assert.assertNull(classpathReference.getGroupId());
        Assert.assertNull(classpathReference.getDerivedNamespace());

        ClasspathReference classpathReferenceTwo = new ClasspathReference(null, null, null);
        Assert.assertTrue(classpathReference.equals(classpathReferenceTwo));
        Assert.assertTrue(classpathReferenceTwo.equals(classpathReference));
        Assert.assertEquals(classpathReference.hashCode(), classpathReferenceTwo.hashCode());


        final URL TEST_URL = new URL("file:/some/url/to/some/jar");

        classpathReference = new ClasspathReference(TEST_URL, TEST_ARTIFACT, null);
        Assert.assertEquals(TEST_ARTIFACT, classpathReference.getArtifact());
        Assert.assertEquals(null, classpathReference.getGroupId());
        Assert.assertNull(classpathReference.getDerivedNamespace());

        classpathReference = new ClasspathReference(TEST_URL, null, TEST_GROUP);
        Assert.assertEquals(null, classpathReference.getArtifact());
        Assert.assertEquals(TEST_GROUP, classpathReference.getGroupId());
        Assert.assertNull(classpathReference.getDerivedNamespace());

        classpathReference = new ClasspathReference(TEST_URL, TEST_ARTIFACT, TEST_GROUP);
        Assert.assertEquals(TEST_URL, classpathReference.getUrl());
        Assert.assertEquals(TEST_ARTIFACT, classpathReference.getArtifact());
        Assert.assertEquals(TEST_GROUP, classpathReference.getGroupId());

        final String testNamespace = TEST_ARTIFACT + "." + TEST_GROUP;
        Assert.assertEquals(testNamespace, classpathReference.getDerivedNamespace());

        Assert.assertFalse(classpathReference.equals(classpathReferenceTwo));
        Assert.assertFalse(classpathReferenceTwo.equals(classpathReference));
        Assert.assertNotEquals(classpathReference.hashCode(), classpathReferenceTwo.hashCode());
    }
}
