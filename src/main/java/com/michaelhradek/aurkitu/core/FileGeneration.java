/**
 * 
 */
package com.michaelhradek.aurkitu.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.michaelhradek.aurkitu.Config;
import com.michaelhradek.aurkitu.core.output.Schema;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
public class FileGeneration {

  private Logger logger = Config.getLogger(getClass());

  private File outputDirectory;

  public FileGeneration(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  void writeSchema(Schema schema) throws IOException {

    if (!outputDirectory.exists()) {
      logger.log(Level.FINE, "File does not exist; creating directories");
      outputDirectory.mkdirs();
    }

    String fileName = "." + Config.FILE_EXTENSION;
    if (schema.getName() == null) {
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
      logger.log(Level.FINE, "Error creating file: " + touch + e.getMessage());
      throw new IOException(e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          logger.log(Level.FINE, "Unable to close writer. " + e.getMessage());
        }
      }
    }
  }
}
