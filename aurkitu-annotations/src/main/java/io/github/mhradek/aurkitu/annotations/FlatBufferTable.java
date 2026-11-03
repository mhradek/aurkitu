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

    TableStructureType value() default TableStructureType.TABLE;\n\n    /**\n     * Optional per-type namespace override.\n     */\n    String namespace() default \"\";

    public enum TableStructureType {
        TABLE, STRUCT
    }
}
