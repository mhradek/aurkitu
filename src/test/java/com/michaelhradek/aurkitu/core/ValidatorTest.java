package com.michaelhradek.aurkitu.core;

import org.junit.Assert;
import org.junit.Test;

import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.Schema;

public class ValidatorTest {

  @Test
  public void testValidateSchema() {
    Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
        .withSourceAnnotation(FlatBufferEnum.class);

    Schema schema = processor.buildSchema();

    Validator validator = new Validator().withSchema(schema);
    validator.validateSchema();
    schema.setValidSchema(validator.getErrors().isEmpty());

    Assert.assertEquals(false, schema.isValidSchema());

    if (Config.DEBUG) {
      System.out.println(validator.getErrorComments());
    }
  }

}
