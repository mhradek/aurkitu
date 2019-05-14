package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

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

        for(Validator.Error error : validator.getErrors()) {
            if(error.getLocation().equals("Option")) {
                Assert.assertEquals(Validator.ErrorType.MISCONFIGURED_DEFINITION, error.getType());
                Assert.assertEquals(null, error.getProperty());
            }

            if(error.getType().equals(Validator.ErrorType.INVALID_PATH)) {
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
}
