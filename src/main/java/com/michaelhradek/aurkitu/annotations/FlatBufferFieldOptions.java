/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.*;

import com.michaelhradek.aurkitu.core.output.FieldType;

/**
 * @author m.hradek
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface FlatBufferFieldOptions {
  FieldType fieldType();
}
