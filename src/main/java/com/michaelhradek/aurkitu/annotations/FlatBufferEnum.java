/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.michaelhradek.aurkitu.core.output.FieldType;

/**
 * @author m.hradek
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferEnum {
  EnumStructureType value() default EnumStructureType.ENUM;

  FieldType enumType() default FieldType.STRING;

  public enum EnumStructureType {
    ENUM, UNION
  }
}
