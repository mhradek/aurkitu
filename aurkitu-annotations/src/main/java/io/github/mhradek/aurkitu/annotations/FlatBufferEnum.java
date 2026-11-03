package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.EnumType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author m.hradek
 * <p>
 * A FlatBuffer Enum definition applies to either a enum or a union.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferEnum {
    EnumStructureType value() default EnumStructureType.ENUM;

    // Defaulting to the smallest size
    EnumType enumType() default EnumType.BYTE;\n\n    /**\n     * Optional per-type namespace override.\n     */\n    String namespace() default "";

    enum EnumStructureType {
        ENUM, UNION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
