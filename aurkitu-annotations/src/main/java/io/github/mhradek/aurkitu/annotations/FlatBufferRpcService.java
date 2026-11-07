package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * On an interface: declares an {@code rpc_service}. Each abstract method taking one table and
 * returning a table becomes a method, e.g. {@code Response store(Request request)} is written as
 * {@code store(Request):Response;}. Mark a method {@code @FlatBufferIgnore} to leave it out.
 *
 * @since 1.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FlatBufferRpcService {

    /** Optional namespace override, as for tables. */
    String namespace() default "";
}
