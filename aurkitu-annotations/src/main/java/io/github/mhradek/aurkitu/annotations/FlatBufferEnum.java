package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.EnumType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares a Java enum as a FlatBuffers {@code enum} (the default) or {@code union}.
 *
 * <p>As an enum, the constants are written in declaration order with implicit values 0, 1, 2, ...;
 * mark a field {@link FlatBufferEnumTypeField} to give them explicit values.
 * As a union, each constant names a table that a field of this union type can hold (see
 * {@link FlatBufferUnionMember}).</p>
 *
 * <pre>{@code
 * @FlatBufferEnum(enumType = EnumType.SHORT)
 * enum Color { RED, GREEN, BLUE }   // enum Color : short { RED, GREEN, BLUE }
 *
 * @FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)
 * enum Equipment { Weapon, Shield } // union Equipment { Weapon, Shield }
 * }</pre>
 *
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferEnum {

    /** {@code ENUM} or {@code UNION}. */
    EnumStructureType value() default EnumStructureType.ENUM;

    /** The enum's underlying integer type, written as {@code enum Name : type}. Ignored for unions. */
    EnumType enumType() default EnumType.BYTE;

    /**
     * The schema namespace of this type, overriding the plugin's {@code schemaNamespace}.
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

    /** Whether the Java enum is written as an {@code enum} or a {@code union}. */
    enum EnumStructureType {
        /** {@code enum}: named integer constants. */
        ENUM,
        /** {@code union}: one of several tables, chosen per value. */
        UNION;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
