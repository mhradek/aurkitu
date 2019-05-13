package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 *
 * Applied to either a Field or a Type, this will cause the parsing to ignore these during schema generation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD, ElementType.TYPE})
public @interface FlatBufferIgnore {

}
