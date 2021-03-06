package com.michaelhradek.aurkitu.plugin.core.output.components;

import org.junit.Assert;
import org.junit.Test;

public class NamespaceTest {

    private static final String NAMESPACE_SEPARATOR = ":";
    private static final String TEST_NAMESPACE_GROUPID = "test_group_id";
    private static final String TEST_NAMESPACE_IDENTIFIER = "test_identifier";
    private static final String TEST_NAMESPACE_ARTIFACTID = "test_artifact_id";

    @Test
    public void testParse() {
        Namespace namespace = Namespace.parse("");
        Assert.assertNull(namespace);

        namespace = Namespace.parse(null);
        Assert.assertNull(namespace);

        namespace = Namespace.parse(TEST_NAMESPACE_GROUPID);
        Assert.assertNotNull(namespace);
        Assert.assertEquals(TEST_NAMESPACE_GROUPID, namespace.getGroupId());
        Assert.assertNull(namespace.getIdentifier());
        Assert.assertNull(namespace.getArtifactId());

        namespace = Namespace.parse(NAMESPACE_SEPARATOR + TEST_NAMESPACE_IDENTIFIER);
        Assert.assertNotNull(namespace);
        Assert.assertNull(namespace.getGroupId());
        Assert.assertEquals(TEST_NAMESPACE_IDENTIFIER, namespace.getIdentifier());
        Assert.assertNull(namespace.getArtifactId());

        namespace = Namespace.parse(NAMESPACE_SEPARATOR + NAMESPACE_SEPARATOR + TEST_NAMESPACE_ARTIFACTID);
        Assert.assertNotNull(namespace);
        Assert.assertNull(namespace.getGroupId());
        Assert.assertNull(namespace.getIdentifier());
        Assert.assertEquals(TEST_NAMESPACE_ARTIFACTID, namespace.getArtifactId());
    }

    @Test
    public void testIsEmpty() {
        Namespace namespace = new Namespace();
        Assert.assertTrue(namespace.isEmpty());

        namespace.setGroupId(TEST_NAMESPACE_GROUPID);
        Assert.assertFalse(namespace.isEmpty());
    }

    @Test
    public void testToString() {
        Namespace namespace = new Namespace();
        Assert.assertEquals("", namespace.toString());

        namespace.setArtifactId(TEST_NAMESPACE_ARTIFACTID);
        Assert.assertEquals(TEST_NAMESPACE_ARTIFACTID, namespace.toString());

        namespace.setIdentifier("iid");
        Assert.assertEquals("iid." + TEST_NAMESPACE_ARTIFACTID, namespace.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        Namespace namespaceA = new Namespace("groupIdA", "identifierA", "artifactIdA");
        Namespace namespaceB = new Namespace("groupIdA", "identifierA", "artifactIdA");

        Assert.assertTrue(namespaceA.equals(namespaceB) && namespaceB.equals(namespaceA));
        Assert.assertTrue(namespaceA.hashCode() == namespaceB.hashCode());

        namespaceB.setGroupId("groupIdB");
        Assert.assertFalse(namespaceA.equals(namespaceB) && namespaceB.equals(namespaceA));
        Assert.assertFalse(namespaceA.hashCode() == namespaceB.hashCode());
    }

    @Test
    public void testStaticIsEmpty() {
        Namespace namespace = null;
        Assert.assertTrue(Namespace.isEmpty(namespace));

        namespace = new Namespace();
        Assert.assertTrue(Namespace.isEmpty(namespace));

        // Most of this testing is done in `isEmpty()`
        namespace.setArtifactId("test.artifact.id");
        Assert.assertFalse(Namespace.isEmpty(namespace));
    }
}