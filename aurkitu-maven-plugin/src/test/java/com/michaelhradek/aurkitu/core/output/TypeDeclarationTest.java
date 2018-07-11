package com.michaelhradek.aurkitu.core.output;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TypeDeclarationTest {

    @Test
    public void testHashCode() {
        TypeDeclaration declarationAlpha = new TypeDeclaration();
        TypeDeclaration declarationBeta = new TypeDeclaration();
        Assert.assertNotEquals(declarationAlpha.hashCode(), declarationBeta.hashCode());
        Assert.assertNotNull(declarationAlpha.hashCode());
        Assert.assertNotNull(declarationBeta.hashCode());

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
        TypeDeclaration declarationAlpha = new TypeDeclaration();
        declarationAlpha.setName("declarationAlpha");

        TypeDeclaration declarationBeta = new TypeDeclaration();
        declarationBeta.setName("declarationBeta");

        List<TypeDeclaration> list = new ArrayList<TypeDeclaration>();
        list.add(declarationBeta);
        Assert.assertTrue(list.contains(declarationBeta));
        Assert.assertFalse(list.contains(declarationAlpha));
        Assert.assertEquals(1, list.size());
    }
}
