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
}
