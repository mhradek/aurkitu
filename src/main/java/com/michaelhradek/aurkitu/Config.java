/**
 * 
 */
package com.michaelhradek.aurkitu;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
public final class Config {

  /**
   * Only use on your local dev box.
   */
  public static final boolean DEBUG = false;

  /**
   * 
   */
  public static final String FILE_EXTENSION = "fbs";

  /**
   * 
   */
  public static final String SCHEMA_INTRO_COMMENT =
      "// Aurkitu automatically generated IDL FlatBuffer Schema";

  public static final String SCHEMA_VERSION_COMMENT =
      "// @version: " + Config.SCHEMA_VERSION_PLACEHOLDER;

  public static final String SCHEMA_VERSION_PLACEHOLDER = "AURKITU-SCHEMA-VERSION-ghjtyu567FHGFD";
}
