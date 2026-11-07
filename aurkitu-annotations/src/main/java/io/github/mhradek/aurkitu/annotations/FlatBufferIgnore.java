package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 * <p>
 * Applied to a field, a nested type or an RPC method, this leaves it out of the schema.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
public @interface FlatBufferIgnore {

}
