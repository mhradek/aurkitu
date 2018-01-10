/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.michaelhradek.aurkitu.core.output.FieldType;

@Retention(CLASS)
@Target({FIELD, PARAMETER, LOCAL_VARIABLE})
/**
 * @author m.hradek
 *
 */
public @interface FlatBufferFieldOptions {
  FieldType fieldType();
}
