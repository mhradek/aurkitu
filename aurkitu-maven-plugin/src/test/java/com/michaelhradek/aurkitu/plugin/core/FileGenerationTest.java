package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author m.hradek
 */
public class FileGenerationTest {

    private static final String OUTPUT_DIRECTORY = "target/aurkitu/test";

    /**
     * Test method for
     * {@link com.michaelhradek.aurkitu.plugin.core.FileGeneration#writeSchema(com.michaelhradek.aurkitu.plugin.core.output.Schema)}.
     *
     * @throws IOException if unable to locate directory/file used in tests
     * @throws MojoExecutionException if an error occurs attempting to execute plugin
     */
    @Test
    public void testWriteSchema() throws IOException, MojoExecutionException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class);
        Assert.assertEquals(2, processor.getSourceAnnotations().size());

        Schema schema = processor.buildSchema();
        schema.setGenerateVersion(true);

        File outputDirectory = new File(OUTPUT_DIRECTORY);
        Assert.assertFalse(outputDirectory.exists());
        Assert.assertFalse(outputDirectory.isDirectory());

        FileGeneration fg = new FileGeneration(outputDirectory);
        fg.writeSchema(schema);
        schema.setName("test");
        fg.writeSchema(schema);

        // TODO Rigorous testing
        Assert.assertTrue(outputDirectory.exists());
        Assert.assertTrue(outputDirectory.isDirectory());

        File resultingFile = new File(OUTPUT_DIRECTORY + File.separator + fg.getFileName());

        if (Config.DEBUG) {
            System.out.println("RESULTING FILE: " + resultingFile.getPath());
        }

        Assert.assertTrue(resultingFile.exists());
        Assert.assertTrue(resultingFile.isFile());
        Assert.assertEquals("test." + Config.FILE_EXTENSION, fg.getFileName());
        Assert.assertEquals(outputDirectory, fg.getOutputDirectory());

        BufferedReader reader =
                new BufferedReader(new FileReader(OUTPUT_DIRECTORY + File.separator + fg.getFileName()));
        List<String> lines = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();

        String[] data = lines.toArray(new String[]{});
        Assert.assertEquals(Config.SCHEMA_INTRO_COMMENT, data[0]);
    }
}
