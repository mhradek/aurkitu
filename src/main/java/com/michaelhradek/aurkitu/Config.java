/**
 * 
 */
package com.michaelhradek.aurkitu;

import java.util.logging.Logger;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
public class Config {

  /**
   * 
   */
  public static final boolean DEBUG = true;

  /**
   * 
   */
  public static final String SEARCdH_PATH = ".*";

  /**
   * 
   */
  public static final String SCHEMA_INTRO_COMMENT =
      "// Aurkitu automatically generated IDL FlatBuffer Schema";

  /**
   * 
   * @param clazz
   * @return
   */
  public static Logger getLogger(Class<?> clazz) {
    return Logger.getLogger(clazz.getName());
  }
}
