package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * On a constant of a {@code @FlatBufferEnum(UNION)} enum: the table this member holds. The
 * constant's name becomes an alias, so one table can appear under several names
 * ({@code Start:MarkerPosition}). Without it, the constant's name must be the table's name.
 *
 * @since 1.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FlatBufferUnionMember {

    /** The member's table (or struct) class. */
    Class<?> value();
}
