package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.lang.annotation.*;

/**
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface FlatBufferFieldOptions {

    /**
     * Required
     *
     * @return FieldType
     */
    FieldType fieldType();

    /**
     * Overrides the default behavior of using the Class#getSimpleName() for names in declarations
     *
     * @return boolean; default false
     */
    boolean useFullName() default false;

    /**
     * While any type is valid it must be entered as a String here
     *
     * @return String; default empty String
     */
    String defaultValue() default "";

    /**
     * Marks the field as required in the generated schema.
     */
    boolean required() default false;

    /**
     * Marks the field as deprecated in the generated schema.
     */
    boolean deprecated() default false;

    /**
     * Optional explicit field id (emitted as id: N).
     * Use -1 to omit.
     */
    int fieldId() default -1;

    /** Emit (key). */
    boolean key() default false;

    /** Emit (hash: "fnv1_32") or similar. Empty to omit. */
    String hash() default "";

    /** Emit (nested_flatbuffer: "Type"). Empty to omit. */
    String nestedFlatbuffer() default "";

    /** Emit (flexbuffer). */
    boolean flexbuffer() default false;

    /** Emit (force_align: N). 0 to omit. */
    int forceAlign() default 0;

    /** For fixed-size arrays in structs: [type:N]. 0 to omit. */
    int fixedLength() default 0;

    /**
     * Custom attributes, e.g. {@code "priority: 1"} or {@code "help: \"text\""}, written after the
     * field. Each name is declared in the schema with {@code attribute "name";}.
     *
     * @since 1.1.0
     */
    String[] attributes() default {};
}
