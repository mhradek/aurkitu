package com.michaelhradek.aurkitu.annotations.types;

/**
 * These are the acceptable types for Enums. Must be integral C/C++ types.
 *
 * @author m.hradek
 */
public enum EnumType {
    BYTE(byte.class), UBYTE(byte.class), SHORT(short.class), USHORT(
            short.class), INT(int.class), UINT(int.class), LONG(long.class), ULONG(
            long.class);

    public Class<?> targetClass;

    EnumType(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
