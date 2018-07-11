package com.michaelhradek.aurkitu.core;

import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

public class ValidatorTest {

    @Test
    public void testValidateSchema() throws MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class);

        Schema schema = processor.buildSchema();

        Validator validator = new Validator().withSchema(schema);
        validator.validateSchema();
        schema.setIsValidSchema(validator.getErrors().isEmpty());

        Assert.assertEquals(false, schema.getIsValidSchema());

        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples
        // Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: dataMap
        // Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: com.michaelhradek.aurkitu.test.SampleClassReferenced$SampleClassTableInnerEnumInt
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
                Assert.assertEquals("Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: com.michaelhradek.aurkitu.test.SampleClassReferenced$SampleClassTableInnerEnumInt", error.getComment());
            }
        }

        if (Config.DEBUG) {
            System.out.println(validator.getErrorComments());
        }
    }

}
