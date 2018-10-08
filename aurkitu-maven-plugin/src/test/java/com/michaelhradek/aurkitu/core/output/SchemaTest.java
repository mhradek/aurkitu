package com.michaelhradek.aurkitu.core.output;

import org.junit.Assert;
import org.junit.Test;

public class SchemaTest {

    @Test
    public void testSetFileIdentifier() {
        Schema schema = new Schema();
        Assert.assertEquals(null, schema.getFileIdentifier());

        schema.setFileIdentifier("INVALID_LENGTH");
        Assert.assertEquals(null, schema.getFileIdentifier());

        final String validFileIdentifier = "VLID";
        schema.setFileIdentifier(validFileIdentifier);
        Assert.assertEquals(validFileIdentifier, schema.getFileIdentifier());
    }

    @Test
    public void testSetFileExtension() {
        Schema schema = new Schema();
        Assert.assertEquals(null, schema.getFileExtension());

        schema.setFileExtension(null);
        Assert.assertEquals(null, schema.getFileExtension());

        schema.setFileExtension("");
        Assert.assertEquals(null, schema.getFileExtension());

        schema.setFileExtension("ABC");
        Assert.assertEquals("abc", schema.getFileExtension());

        schema.setFileExtension(null);
        Assert.assertEquals(null, schema.getFileExtension());

        schema.setFileExtension("");
        Assert.assertEquals(null, schema.getFileExtension());
    }

    @Test
    public void testToString() {
        final String INCLUDE_1 = "../../../some/file.fbs";
        final String INCLUDE_2 = "../../../another/file.fbs;";
        final String ATTRIBUTE = "someAttribute";
        final String INT_CONST_NAME = "int_const_name";
        final int INT_CONST_VALUE = 44;
        final String FLOAT_CONST_NAME = "float_const_name";
        final float FLOAT_CONST_VALUE = 34.2F;
        final String FILE_ID = "AB43";
        final String FILE_EXT = "fbs";
        final String NAMESPACE = "com.some.namespace";

        Schema schema = new Schema();
        schema.addInclude(INCLUDE_1);
        schema.addInclude(INCLUDE_2);
        schema.addAttribute(ATTRIBUTE);

        Schema.Constant<Integer> integerConstant = new Schema.Constant<Integer>();
        integerConstant.name = INT_CONST_NAME;
        integerConstant.value = INT_CONST_VALUE;
        schema.addIntegerConstant(integerConstant);

        Schema.Constant<Float> floatConstant = new Schema.Constant<Float>();
        floatConstant.name = FLOAT_CONST_NAME;
        floatConstant.value = FLOAT_CONST_VALUE;
        schema.addFloatConstant(floatConstant);

        schema.setFileIdentifier(FILE_ID);
        schema.setFileExtension(FILE_EXT);

        schema.setNamespace(NAMESPACE);

        for(String line : schema.toString().split(System.lineSeparator())) {
            if(line.contains(INCLUDE_1)) {
                Assert.assertEquals("include " + INCLUDE_1 + ";", line);
                continue;
            }

            if(line.contains(INCLUDE_2)) {
                Assert.assertEquals("include " + INCLUDE_2, line);
                continue;
            }

            if(line.contains(INCLUDE_2)) {
                Assert.assertEquals("include " + INCLUDE_2, line);
                continue;
            }

            if(line.contains(ATTRIBUTE)) {
                Assert.assertEquals("attribute \"" + ATTRIBUTE + "\";", line);
                continue;
            }

//            if(line.contains(INT_CONST_NAME)) {
//                Assert.assertEquals("attribute " + ATTRIBUTE, line);
//                continue;
//            }
//
//            if(line.contains(FLOAT_CONST_NAME)) {
//                Assert.assertEquals("attribute " + ATTRIBUTE, line);
//                continue;
//            }

            if(line.contains(FILE_ID)) {
                Assert.assertEquals("file_identifier \"" + FILE_ID + "\";", line);
                continue;
            }

            if(line.contains(FILE_EXT)) {
                Assert.assertEquals("file_extension \"" + FILE_EXT + "\";", line);
                continue;
            }

            if(line.contains(NAMESPACE)) {
                Assert.assertEquals("namespace " + NAMESPACE + ";", line);
                continue;
            }
        }
    }

    @Test
    public void testAddTypeDeclaration() {
        Schema schema = new Schema();
        TypeDeclaration declarationAlpha = new TypeDeclaration();
        declarationAlpha.setName("alpha");
        schema.addTypeDeclaration(declarationAlpha);
        Assert.assertEquals(1, schema.getTypes().size());

        TypeDeclaration declarationBeta = new TypeDeclaration();
        declarationBeta.setName("alpha"); // Testing collision
        schema.addTypeDeclaration(declarationBeta);
        Assert.assertEquals(1, schema.getTypes().size());

        declarationBeta.setName("beta"); // Testing collision
        schema.addTypeDeclaration(declarationBeta);
        Assert.assertEquals(2, schema.getTypes().size());
    }
}