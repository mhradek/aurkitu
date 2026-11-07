/**
 *
 */
package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 *
 * A FlatBuffer Table definition applies to either a class or a struct.
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface FlatBufferTable {
    boolean rootType() default false;

    TableStructureType value() default TableStructureType.TABLE;

    /**
     * Optional per-type namespace override.
     */
    String namespace() default "";

    /**
     * Tables only: keep fields in declaration order in the buffer (original_order).
     *
     * @since 1.1.0
     */
    boolean originalOrder() default false;

    /**
     * Structs only: force the struct's alignment to this power of two (force_align). 0 to omit.
     *
     * @since 1.1.0
     */
    int forceAlign() default 0;

    /**
     * Custom attributes written after the type name, e.g. {@code "priority: 1"}.
     *
     * @since 1.1.0
     */
    String[] attributes() default {};

    public enum TableStructureType {
        TABLE, STRUCT
    }
}
