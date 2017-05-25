/**
 * 
 */
package com.michaelhradek.aurkitu.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(CLASS)
@Target({FIELD, PARAMETER, LOCAL_VARIABLE})
/**
 * @author m.hradek
 * @date May 24, 2017
 *   
 */
public @interface BogusAnnotation {

}
