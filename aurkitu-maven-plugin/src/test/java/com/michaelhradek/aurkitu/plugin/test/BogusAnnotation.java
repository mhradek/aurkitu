package com.michaelhradek.aurkitu.plugin.test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(CLASS)
@Target({FIELD, PARAMETER, LOCAL_VARIABLE})
/**
 * @author m.hradek
 * @date May 24, 2017
 *
 */
public @interface BogusAnnotation {

}
