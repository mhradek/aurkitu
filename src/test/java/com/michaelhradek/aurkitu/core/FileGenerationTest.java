/**
 * 
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.Schema;

import junit.framework.Assert;

/**
 * @author m.hradek
 * @date May 25, 2017
 * 
 */
public class FileGenerationTest {

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {}

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {}

  /**
   * Test method for
   * {@link com.michaelhradek.aurkitu.core.FileGeneration#writeSchema(com.michaelhradek.aurkitu.core.output.Schema)}.
   * 
   * @throws IOException
   */
  @Test
  public void testWriteSchema() throws IOException {
    Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
        .withSourceAnnotation(FlatBufferEnum.class);
    Assert.assertEquals(2, processor.getSourceAnnotations().size());

    Schema schema = processor.buildSchema();
    schema.setGenerateVersion(true);

    FileGeneration fg = new FileGeneration(new File("target/aurkito"));
    fg.writeSchema(schema);
    schema.setName("test");
    fg.writeSchema(schema);

    // TODO Rigorous testing
  }

}
