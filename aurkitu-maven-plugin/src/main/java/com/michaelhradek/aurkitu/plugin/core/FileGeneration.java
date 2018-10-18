package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.plugin.Application;
import com.michaelhradek.aurkitu.plugin.Config;
import com.michaelhradek.aurkitu.plugin.core.output.Schema;
import lombok.Getter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author m.hradek
 *
 */
@Getter
public class FileGeneration {

    private File outputDirectory;
    private String fileName;

    public FileGeneration(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void writeSchema(Schema schema) throws IOException {

        if (!outputDirectory.exists()) {
            Application.getLogger().debug("File does not exist; creating directories");
            outputDirectory.mkdirs();
        }

        fileName = "." + Config.FILE_EXTENSION;
        if (schema.getName() == null || schema.getName().length() < 1) {
            fileName = System.currentTimeMillis() + fileName;
        } else {
            fileName = schema.getName() + fileName;
        }

        File touch = new File(outputDirectory, fileName);

        FileWriter writer = null;
        try {
            writer = new FileWriter(touch);
            writer.write(schema.toString());
        } catch (IOException e) {
            Application.getLogger().error("Error creating file: " + touch, e);
            throw new IOException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Application.getLogger().error("Unable to close writer.", e);
                }
            }
        }
    }
}
