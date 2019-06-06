package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class ValidatorTest {

    @Test
    public void testValidateSchema() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());

        processor.execute();

        Schema schema = processor.getProcessedSchemas().get(0);

        Validator validator = new Validator().withSchema(schema);
        validator.validateSchema();
        schema.setIsValid(validator.getErrors().isEmpty());

        Assert.assertEquals(false, schema.getIsValid());

        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples
        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: dataMap
        // Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name
        // contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended:
        // SampleClassReferenced$SampleClassTableInnerEnumInt
        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTableWithUndefined, Name: awesomeUndefinedClass
        // Issue : MISCONFIGURED_DEFINITION, Location: SampleClassTableInnerEnumInt, Name: null
        // Issue : MISCONFIGURED_DEFINITION, Location: Option, Name: null

        for (Validator.Error error : validator.getErrors()) {
            if (error.getLocation().equals("Option")) {
                Assert.assertEquals(Validator.ErrorType.MISCONFIGURED_DEFINITION, error.getType());
                Assert.assertEquals(null, error.getProperty());
            }

            if (error.getType().equals(Validator.ErrorType.INVALID_PATH)) {
                Assert.assertEquals("SampleClassTable", error.getLocation());
                Assert.assertEquals("definedInnerEnumArray", error.getProperty().name);
                Assert.assertEquals("Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on " +
                        "inner not recommended: com.michaelhradek.aurkitu.plugin.test" +
                        ".SampleClassReferenced$SampleClassTableInnerEnumInt", error.getComment());

                Assert.assertEquals("// Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced$SampleClassTableInnerEnumInt\n", error.toString());
            }
        }

        if (Config.DEBUG) {
            System.out.println(validator.getErrorComments());
        }

        final String errorComments = validator.getErrorComments();
        Assert.assertTrue(errorComments.contains("// Schema failed validation (i.e. flatc will likely fail): \n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassReferenced, Name: samples\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTableWithUndefined, Name: awesomeUndefinedClass\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: MapValueSet_SampleClassTable_dataMap, Name: key\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: MapValueSet_SampleClassTable_dataMap, Name: value\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: tokens\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: options\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples\n"));
        Assert.assertTrue(errorComments.contains("// Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced$SampleClassTableInnerEnumInt\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: definedInnerEnumArray\n"));
        Assert.assertTrue(errorComments.contains("// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: fullnameClass\n"));
        Assert.assertTrue(errorComments.contains("// Issue : MISCONFIGURED_DEFINITION, Location: SampleClassTableInnerEnumInt, Name: null\n"));
        Assert.assertTrue(errorComments.contains("// Issue : ENUM_DEFINITION_NOT_DEFINED, Location: TestEnumCommentEmpty, Name: null, Comment: The enum contains no values.\n"));
        Assert.assertTrue(errorComments.contains("// Issue : MISCONFIGURED_DEFINITION, Location: Option, Name: null\n"));
    }

    @Test
    public void testValidateSchemaNullSchema() {
        Validator validator = new Validator();
        Assert.assertNull(validator.getSchema());
        validator.validateSchema();
        Assert.assertNull(validator.getSchema());
        validator.withSchema(null).validateSchema();
        Assert.assertNull(validator.getSchema());
    }

    @Test
    public void testGetErrorComments() {
        Validator validator = new Validator();
        Assert.assertTrue(validator.getErrors().isEmpty());
        Assert.assertTrue(validator.getErrorComments().equalsIgnoreCase("// Schema passed validation"));
    }

    @Test
    public void testValidateSchemaCheckTables() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());

        processor.execute();

        Schema schema = processor.getProcessedSchemas().get(0);

        Validator validator = new Validator().withSchema(schema);

        Assert.assertTrue(validator.isCheckTables());
        Assert.assertTrue(validator.isCheckEnums());
        Assert.assertEquals(0, validator.getErrors().size());

        validator.setCheckTables(false);
        validator.setCheckEnums(false);

        Assert.assertFalse(validator.isCheckTables());
        Assert.assertFalse(validator.isCheckEnums());
        validator.validateSchema();

        Assert.assertEquals(0, validator.getErrors().size());
    }

    @Test
    public void testValidateNamespace() {
        Schema schema =  new Schema();
        Validator validator = new Validator().withSchema(schema).withCheckEnums(false).withCheckTables(false);

        validator.validateSchema();

        Assert.assertEquals(0, validator.getErrors().size());

        schema.setNamespace("");
        validator.setSchema(schema);
        validator.validateSchema();
        Assert.assertEquals(0, validator.getErrors().size());

        schema.setNamespace("test-valid-namespace.com.michaelhradek");
        validator.setSchema(schema);
        validator.validateSchema();

        Assert.assertEquals(0, validator.getErrors().size());

        schema.setNamespace("test-invalid-nam3space.com.michaelhradek");
        validator.setSchema(schema);
        validator.validateSchema();
        Assert.assertEquals(1, validator.getErrors().size());
        Assert.assertEquals(Validator.ErrorType.INVALID_NAMESPACE, validator.getErrors().get(0).getType());

        validator.setErrors(new ArrayList<>());
        validator.setCheckNamespace(false);
        validator.validateSchema();
        Assert.assertEquals(0, validator.getErrors().size());
    }
}
