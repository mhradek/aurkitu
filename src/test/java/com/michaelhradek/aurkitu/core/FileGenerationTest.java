/**
 *
 */
package com.michaelhradek.aurkitu.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.Schema;

/**
 * @author m.hradek
 */
public class FileGenerationTest {

    private static String OUTPUT_DIRECTORY = "target/aurkito";

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

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

        File outputDirectory = new File(OUTPUT_DIRECTORY);
        FileGeneration fg = new FileGeneration(outputDirectory);
        fg.writeSchema(schema);
        schema.setName("test");
        fg.writeSchema(schema);

        // TODO Rigorous testing
        Assert.assertEquals(true, outputDirectory.exists());
        Assert.assertEquals(true, outputDirectory.isDirectory());

        File resultingFile = new File(OUTPUT_DIRECTORY + File.separator + fg.getFileName());

        if (Config.DEBUG) {
            System.out.println("RESULTING FILE: " + resultingFile.getPath());
        }

        Assert.assertEquals(true, resultingFile.exists());
        Assert.assertEquals(true, resultingFile.isFile());
        Assert.assertEquals("test." + Config.FILE_EXTENSION, fg.getFileName());
        Assert.assertEquals(outputDirectory, fg.getOutputDirectory());

        BufferedReader reader =
                new BufferedReader(new FileReader(OUTPUT_DIRECTORY + File.separator + fg.getFileName()));
        List<String> lines = new ArrayList<String>();

        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();

        String[] data = lines.toArray(new String[]{});
        Assert.assertEquals(Config.SCHEMA_INTRO_COMMENT, data[0]);
    }
}
