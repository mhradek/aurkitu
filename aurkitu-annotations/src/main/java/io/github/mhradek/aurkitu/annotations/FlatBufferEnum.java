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
    EnumType enumType() default EnumType.BYTE;

    /**
     * Optional per-type namespace override.
     */
    String namespace() default "";

    /**
     * Enums with an unsigned underlying type only: values are bit positions (bit_flags), so an
     * explicit value N means 1 &lt;&lt; N and implicit values give 1, 2, 4, 8, ...
     *
     * @since 1.1.0
     */
    boolean bitFlags() default false;

    /**
     * Custom attributes written after the enum or union name, e.g. {@code "priority: 1"}.
     *
     * @since 1.1.0
     */
    String[] attributes() default {};

    enum EnumStructureType {
        ENUM, UNION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
