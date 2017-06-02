/**
 * 
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.michaelhradek.aurkitu.Application;
import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.core.output.Schema;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
public class FileGeneration {

  private File outputDirectory;

  public FileGeneration(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public void writeSchema(Schema schema) throws IOException {

    if (!outputDirectory.exists()) {
      Application.getLogger().debug("File does not exist; creating directories");
      outputDirectory.mkdirs();
    }

    String fileName = "." + Config.FILE_EXTENSION;
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
