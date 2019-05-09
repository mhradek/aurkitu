package com.michaelhradek.aurkitu.plugin.core.output;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EnumDeclarationTest {

    private static final String TEST_COMMENT = "Comment for test purposes";

    @Test
    public void testConstructor() {
        EnumDeclaration declaration = new EnumDeclaration();
        Assert.assertEquals(FlatBufferEnum.EnumStructureType.ENUM, declaration.getStructure());

        declaration = new EnumDeclaration(FlatBufferEnum.EnumStructureType.UNION);
        Assert.assertEquals(FlatBufferEnum.EnumStructureType.UNION, declaration.getStructure());
    }

    @Test
    public void testHashCode() {
        EnumDeclaration declarationAlpha = new EnumDeclaration();
        EnumDeclaration declarationBeta = new EnumDeclaration();
        Assert.assertEquals(declarationAlpha.hashCode(), declarationBeta.hashCode());
        Assert.assertNotEquals(0, declarationAlpha.hashCode());
        Assert.assertNotEquals(0, declarationBeta.hashCode());

        declarationAlpha.setName("declarationAlpha");
        declarationBeta.setName("declarationAlpha");
        Assert.assertEquals(declarationAlpha.hashCode(), declarationBeta.hashCode());
        Assert.assertTrue(declarationAlpha.equals(declarationBeta));
        Assert.assertTrue(declarationBeta.equals(declarationAlpha));

        declarationBeta.setName("declarationBeta");
        Assert.assertNotEquals(declarationAlpha.hashCode(), declarationBeta.hashCode());
        Assert.assertFalse(declarationAlpha.equals(declarationBeta));
        Assert.assertFalse(declarationBeta.equals(declarationAlpha));
    }

    @Test
    public void testEquals() {
        EnumDeclaration declarationAlpha = new EnumDeclaration();
        declarationAlpha.setName("declarationAlpha");

        EnumDeclaration declarationBeta = new EnumDeclaration();
        declarationBeta.setName("declarationBeta");

        List<EnumDeclaration> list = new ArrayList<>();
        list.add(declarationBeta);
        Assert.assertTrue(list.contains(declarationBeta));
        Assert.assertFalse(list.contains(declarationAlpha));
        Assert.assertEquals(1, list.size());

        Assert.assertFalse(declarationAlpha.equals(UUID.randomUUID()));
        Assert.assertNotEquals(declarationAlpha, declarationBeta);
        Assert.assertFalse(declarationAlpha.equals(declarationBeta));
        Assert.assertFalse(declarationBeta.equals(declarationAlpha));
        Assert.assertFalse(declarationAlpha.equals(null));
        Assert.assertFalse(declarationBeta.equals(null));
    }

    @Test
    public void testCommentLogicToString() {
        EnumDeclaration declaration = new EnumDeclaration();
        Assert.assertEquals("enum null { }\n\n", declaration.toString());

        Assert.assertNull(declaration.getComment());
        declaration.setComment("");
        Assert.assertNotNull(declaration.getComment());
        Assert.assertEquals("enum null { }\n\n", declaration.toString());

        declaration.setComment(TEST_COMMENT);
        Assert.assertEquals("// " + TEST_COMMENT + "\nenum null { }\n\n", declaration.toString());
    }
}
