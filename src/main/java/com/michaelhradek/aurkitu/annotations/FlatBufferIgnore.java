/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, PARAMETER, LOCAL_VARIABLE})
/**
 * @author m.hradek
 *
 */
public @interface FlatBufferIgnore {

}
