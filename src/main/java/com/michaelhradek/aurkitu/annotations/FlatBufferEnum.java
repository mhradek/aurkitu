package com.michaelhradek.aurkitu.annotations;

import com.michaelhradek.aurkitu.core.output.EnumType;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author m.hradek
 *
 * A FlatBuffer Enum definition applies to either a enum or a union.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferEnum {
  EnumStructureType value() default EnumStructureType.ENUM;

    // Defaulting to the smallest size
    EnumType enumType() default EnumType.BYTE;

    enum EnumStructureType {
        ENUM, UNION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
  }
}
