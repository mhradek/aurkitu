/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE})
public @interface FlatBufferTable {
    boolean rootType() default false;

    TableStructureType value() default TableStructureType.TABLE;

    public enum TableStructureType {
        TABLE, STRUCT
    }
}
