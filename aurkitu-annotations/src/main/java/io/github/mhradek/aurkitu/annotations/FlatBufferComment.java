package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * Documentation for a type, field, enum constant, union member or RPC method, written as
 * {@code ///} comment lines (one per line of text) so flatc copies it into generated code.
 *
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE,
        ElementType.METHOD})
public @interface FlatBufferComment {

    /** The text. Each line becomes its own {@code ///} line. */
    String comment();
}
