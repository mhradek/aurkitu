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

        if (Config.DEBUG) {
            System.out.println(validator.getErrorComments());
        }
    }

}
