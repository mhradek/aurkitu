package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * Applied to a field, a nested type or an RPC method, this leaves it out of the schema.
 *
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
public @interface FlatBufferIgnore {

}
