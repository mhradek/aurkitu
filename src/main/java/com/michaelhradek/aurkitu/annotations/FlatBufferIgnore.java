/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD, ElementType.TYPE})
public @interface FlatBufferIgnore {

}
