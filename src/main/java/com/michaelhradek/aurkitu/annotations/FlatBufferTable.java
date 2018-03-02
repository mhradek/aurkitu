/**
 *
 */
package com.michaelhradek.aurkitu.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FlatBufferTable {
    boolean rootType() default false;

    TableStructureType value() default TableStructureType.TABLE;

    public enum TableStructureType {
        TABLE, STRUCT
    }
}
