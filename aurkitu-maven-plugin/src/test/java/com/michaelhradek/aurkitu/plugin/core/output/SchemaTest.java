package com.michaelhradek.aurkitu.plugin.core.output;

import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.Processor;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class SchemaTest {

    private static final String TEST_SCHEMA_HEADER = Config.SCHEMA_INTRO_COMMENT + "\n\n";
    private static final String TEST_NAMESPACE = "com.company.test.package";

    @Test
    public void testSetFileIdentifier() {
        Schema schema = new Schema();
        Assert.assertNull(schema.getFileIdentifier());

        schema.setFileIdentifier("INVALID_LENGTH");
        Assert.assertNull(schema.getFileIdentifier());

        final String validFileIdentifier = "VLID";
        schema.setFileIdentifier(validFileIdentifier);
        Assert.assertEquals(validFileIdentifier, schema.getFileIdentifier());

        schema.setFileIdentifier(null);
        Assert.assertNull(schema.getFileIdentifier());
    }

    @Test
    public void testSetFileExtension() {
        Schema schema = new Schema();
        Assert.assertNull(schema.getFileExtension());

        schema.setFileExtension(null);
        Assert.assertNull(schema.getFileExtension());

        schema.setFileExtension("");
        Assert.assertNull(schema.getFileExtension());

        schema.setFileExtension("ABC");
        Assert.assertEquals("abc", schema.getFileExtension());

        schema.setFileExtension(null);
        Assert.assertNull(schema.getFileExtension());

        schema.setFileExtension("");
        Assert.assertNull(schema.getFileExtension());
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

        Schema.Constant<Integer> integerConstant = new Schema.Constant<>();
        integerConstant.name = INT_CONST_NAME;
        integerConstant.value = INT_CONST_VALUE;
        schema.addIntegerConstant(integerConstant);

        Schema.Constant<Float> floatConstant = new Schema.Constant<>();
        floatConstant.name = FLOAT_CONST_NAME;
        floatConstant.value = FLOAT_CONST_VALUE;
        schema.addFloatConstant(floatConstant);

        schema.setFileIdentifier(FILE_ID);
        schema.setFileExtension(FILE_EXT);

        schema.setNamespace(NAMESPACE);

        for (String line : schema.toString().split(System.lineSeparator())) {
            if (line.contains(INCLUDE_1)) {
                Assert.assertEquals("include " + INCLUDE_1 + ";", line);
                continue;
            }

            if (line.contains(INCLUDE_2)) {
                Assert.assertEquals("include " + INCLUDE_2, line);
                continue;
            }

            if (line.contains(ATTRIBUTE)) {
                Assert.assertEquals("attribute \"" + ATTRIBUTE + "\";", line);
                continue;
            }

            if (line.contains(INT_CONST_NAME)) {
                Assert.assertEquals("int " + INT_CONST_NAME + " " + INT_CONST_VALUE + ";", line);
                continue;
            }

            if (line.contains(FLOAT_CONST_NAME)) {
                Assert.assertEquals("float " + FLOAT_CONST_NAME + " " + FLOAT_CONST_VALUE + ";", line);
                continue;
            }

            if (line.contains(FILE_ID)) {
                Assert.assertEquals("file_identifier \"" + FILE_ID + "\";", line);
                continue;
            }

            if (line.contains(FILE_EXT)) {
                Assert.assertEquals("file_extension \"" + FILE_EXT + "\";", line);
                continue;
            }

            if (line.contains(NAMESPACE)) {
                Assert.assertEquals("namespace " + NAMESPACE + ";", line);
                continue;
            }

            // Ignore these
            if (line.contains(Config.SCHEMA_INTRO_COMMENT) || line.equalsIgnoreCase("")) {
                continue;
            }

            Assert.fail("Line not tested: [" + line + "]");
        }
    }

    @Test
    public void testAttributesToString() {
        Schema schema = new Schema();
        Assert.assertTrue(schema.getAttributes().isEmpty());
        Assert.assertEquals(TEST_SCHEMA_HEADER, schema.toString());
        schema.setAttributes(null);
        Assert.assertNull(schema.getAttributes());
        Assert.assertEquals(TEST_SCHEMA_HEADER, schema.toString());
        schema.setAttributes(new ArrayList<>());
        Assert.assertNotNull(schema.getAttributes());
        Assert.assertEquals(TEST_SCHEMA_HEADER, schema.toString());

        schema.addAttribute("Test Attribute");
        Assert.assertEquals(TEST_SCHEMA_HEADER + "attribute \"Test Attribute\";\n\n", schema.toString());
    }

    @Test
    public void testNamespaceToString() {
        Schema schema = new Schema();
        Assert.assertNull(schema.getNamespace());
        Assert.assertEquals(TEST_SCHEMA_HEADER, schema.toString());

        schema.setNamespace(TEST_NAMESPACE);
        Assert.assertEquals(TEST_SCHEMA_HEADER + "namespace " + TEST_NAMESPACE + ";\n\n", schema.toString());

        schema.setNamespace(TEST_NAMESPACE + ";");
        Assert.assertEquals(TEST_SCHEMA_HEADER + "namespace " + TEST_NAMESPACE + ";\n\n", schema.toString());
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

    @Test
    public void testFields() {
        Schema schema = new Schema();

        Assert.assertNotNull(schema.getIncludes());
        Assert.assertTrue(schema.getIncludes().isEmpty());

        Assert.assertNotNull(schema.getIntegerConstants());
        Assert.assertTrue(schema.getIntegerConstants().isEmpty());

        Assert.assertNotNull(schema.getFloatConstants());
        Assert.assertTrue(schema.getFloatConstants().isEmpty());

        Assert.assertFalse(schema.isGenerateVersion());
        Assert.assertNull(schema.getValidator());
    }

    @Test
    public void testEquals() {
        Schema schemaOne = new Schema();
        Schema schemaTwo = new Schema();

        Assert.assertFalse(schemaOne.equals(null));
        Assert.assertFalse(schemaOne.equals(String.class));
        Assert.assertFalse(schemaOne.equals(new Processor()));
        Assert.assertTrue(schemaOne.equals(schemaTwo));

        schemaOne.setNamespace(schemaOne.getClass().getPackage().getName());
        Assert.assertFalse(schemaOne.equals(schemaTwo));
        schemaTwo.setNamespace(schemaTwo.getClass().getPackage().getName());
        Assert.assertTrue(schemaOne.equals(schemaTwo));

        schemaOne.setNamespace("nameOne");
        Assert.assertFalse(schemaOne.equals(schemaTwo));
        schemaTwo.setNamespace("nameOne");
        Assert.assertTrue(schemaOne.equals(schemaTwo));
    }

    @Test
    public void testGenerateVersion() {
        Schema schema = new Schema();
        Assert.assertFalse(schema.toString().contains("// @version:"));
        schema.setGenerateVersion(true);
        Assert.assertTrue(schema.toString().contains("// @version:"));
    }

    @Test
    public void testIsEmpty() {
        Schema schema = new Schema();
        Assert.assertFalse(schema.isEmpty());
        schema.isEmpty(true);
        Assert.assertTrue(schema.isEmpty());
    }
}