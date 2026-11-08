package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * On the one field of a {@link FlatBufferEnum} enum that holds each constant's value: those values are
 * written explicitly, in ascending order, instead of the implicit 0, 1, 2, ...
 *
 * <pre>{@code
 * @FlatBufferEnum
 * enum Status {
 *     ACTIVE(1), CLOSED(5);
 *     @FlatBufferEnumTypeField final int code;
 *     Status(int code) { this.code = code; }
 * }   // enum Status : byte { ACTIVE = 1, CLOSED = 5 }
 * }</pre>
 *
 * <p>The field must be a {@code byte}, {@code short}, {@code int} or {@code long}. For an unsigned
 * {@code enumType}, a negative value is read as unsigned at that width ({@code (byte) -1} is 255).</p>
 *
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferEnumTypeField {

}
