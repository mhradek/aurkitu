package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 * <p>
 * Used to add comments to any Type, Enum, Field, etc.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE})
public @interface FlatBufferComment {

    String comment();
}
