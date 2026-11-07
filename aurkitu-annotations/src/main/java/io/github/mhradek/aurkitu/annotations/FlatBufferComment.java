package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 * <p>
 * Documentation for a type, field, enum constant, union member or RPC method, written as
 * {@code ///} comment lines (one per line of text) so flatc copies it into generated code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE,
        ElementType.METHOD})
public @interface FlatBufferComment {

    String comment();
}
