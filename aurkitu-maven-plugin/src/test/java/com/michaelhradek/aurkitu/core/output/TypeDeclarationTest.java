package com.michaelhradek.aurkitu.core.output;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TypeDeclarationTest {

    private static final String TEST_COMMENT = "Comment for test purposes";
    private static final String TEST_MAPVALUESET_KEY = "Test Key";
    private static final String TEST_MAPVALUESET_VALUE = "Test Value";

    @Test
    public void testConstructor() {
        TypeDeclaration declaration = new TypeDeclaration();
        Assert.assertEquals(FlatBufferTable.TableStructureType.TABLE, declaration.getStructure());

        declaration = new TypeDeclaration(FlatBufferTable.TableStructureType.STRUCT);
        Assert.assertEquals(FlatBufferTable.TableStructureType.STRUCT, declaration.getStructure());
    }

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

        Assert.assertFalse(declarationAlpha.equals(UUID.randomUUID()));
    }

    @Test
    public void testCommentLogicToString() {
        TypeDeclaration declaration = new TypeDeclaration();
        Assert.assertEquals("table null {\n}\n\n", declaration.toString());

        Assert.assertNull(declaration.getComment());
        declaration.setComment("");
        Assert.assertNotNull(declaration.getComment());
        Assert.assertEquals("table null {\n}\n\n", declaration.toString());

        declaration.setComment(TEST_COMMENT);
        Assert.assertEquals("// " + TEST_COMMENT + "\ntable null {\n}\n\n", declaration.toString());
    }

    @Test
    public void testMapValueSet() {
        TypeDeclaration.MapValueSet mapValueSet = new TypeDeclaration.MapValueSet();
        Assert.assertNotNull(mapValueSet);
        Assert.assertNull(mapValueSet.key);
        Assert.assertNull(mapValueSet.value);

        mapValueSet.key = TEST_MAPVALUESET_KEY;
        mapValueSet.value = TEST_MAPVALUESET_VALUE;

        Assert.assertNotNull(mapValueSet.key);
        Assert.assertNotNull(mapValueSet.value);

        Assert.assertEquals(TEST_MAPVALUESET_KEY, mapValueSet.key);
        Assert.assertEquals(TEST_MAPVALUESET_VALUE, mapValueSet.value);
    }
}
