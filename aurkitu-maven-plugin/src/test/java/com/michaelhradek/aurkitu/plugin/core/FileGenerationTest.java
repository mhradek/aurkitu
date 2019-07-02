package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author m.hradek
 */
@RunWith(PowerMockRunner.class)
public class FileGenerationTest {

    private static final String OUTPUT_DIRECTORY_VALID = "target/aurkitu/test";
    private static final String OUTPUT_FILENAME = "test";

    /**
     * Test method for
     * {@link com.michaelhradek.aurkitu.plugin.core.FileGeneration#writeSchema(com.michaelhradek.aurkitu.plugin.core.output.Schema)}.
     *
     * @throws IOException            if unable to locate directory/file used in tests
     * @throws MojoExecutionException if an error occurs attempting to execute plugin
     */
    @Test
    public void testWriteSchema() throws IOException, MojoExecutionException, NoSuchFieldException, IllegalAccessException {
        Processor processor = new Processor().withSourceAnnotation(FlatBufferTable.class)
                .withSourceAnnotation(FlatBufferEnum.class).withSchema(new Schema());
        Assert.assertEquals(2, processor.getSourceAnnotations().size());

        processor.execute();
        Schema schema = processor.getProcessedSchemas().get(0);
        schema.setGenerateVersion(true);

        File outputDirectory = new File(OUTPUT_DIRECTORY_VALID);
        Assert.assertFalse(outputDirectory.exists());
        Assert.assertFalse(outputDirectory.isDirectory());

        FileGeneration fg = new FileGeneration(outputDirectory);
        fg.writeSchema(schema);
        schema.setName(OUTPUT_FILENAME);
        fg.writeSchema(schema);

        Field field = fg.getClass().getDeclaredField("fileName");
        field.setAccessible(true);
        final String fileName = (String) field.get(fg);

        Assert.assertEquals(OUTPUT_FILENAME + "." + Config.FILE_EXTENSION, fileName);

        // TODO Rigorous testing
        Assert.assertTrue(outputDirectory.exists());
        Assert.assertTrue(outputDirectory.isDirectory());

        File resultingFile = new File(OUTPUT_DIRECTORY_VALID + File.separator + fg.getFileName());

        if (Config.DEBUG) {
            System.out.println("RESULTING FILE: " + resultingFile.getPath());
        }

        Assert.assertTrue(resultingFile.exists());
        Assert.assertTrue(resultingFile.isFile());
        Assert.assertEquals("test." + Config.FILE_EXTENSION, fg.getFileName());
        Assert.assertEquals(outputDirectory, fg.getOutputDirectory());

        BufferedReader reader =
                new BufferedReader(new FileReader(OUTPUT_DIRECTORY_VALID + File.separator + fg.getFileName()));
        List<String> lines = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();

        String[] data = lines.toArray(new String[]{});
        Assert.assertEquals(Config.SCHEMA_INTRO_COMMENT, data[0]);
    }

    @Test
    @PrepareForTest({FileWriter.class})
    public void testExceptionHandling() throws Exception {
        File outputDirectory = new File(OUTPUT_DIRECTORY_VALID);
        Schema schema = new Schema();

        FileWriter fileWriter = PowerMockito.mock(FileWriter.class);
        PowerMockito.when(fileWriter, PowerMockito.method(FileWriter.class, "write", String.class)).withArguments(schema.toString()).thenAnswer(invocation -> {
            throw new IOException();
        });

        FileGeneration fg = new FileGeneration(outputDirectory);
        fg.writeSchema(schema);
    }
}
