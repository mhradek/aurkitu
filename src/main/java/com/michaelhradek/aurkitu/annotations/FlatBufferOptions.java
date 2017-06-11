/**
 * 
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author m.hradek
 * @date May 17, 2017
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferOptions {
  boolean rootType() default false;

  TableStructureType value() default TableStructureType.TABLE;

  public enum TableStructureType {
    TABLE, STRUCT
  }
}
