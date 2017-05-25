/**
 * 
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.michaelhradek.aurkitu.core.output.FieldType;

/**
 * @author m.hradek
 * @date May 17, 2017
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferEnum {
  EnumStructureType value() default EnumStructureType.ENUM;

  FieldType enumType() default FieldType.STRING;

  public enum EnumStructureType {
    ENUM, UNION
  }
}
