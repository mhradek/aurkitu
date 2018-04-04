/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.michaelhradek.aurkitu.core.output.FieldType;

/**
 * @author m.hradek
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface FlatBufferFieldOptions {
    FieldType fieldType();

    boolean useFullName() default false;
}
