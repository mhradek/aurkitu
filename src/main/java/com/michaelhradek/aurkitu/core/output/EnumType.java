/**
 *
 */
package com.michaelhradek.aurkitu.core.output;

/**
 * @author m.hradek
 */
public enum EnumType {
    ENUM, UNION;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
