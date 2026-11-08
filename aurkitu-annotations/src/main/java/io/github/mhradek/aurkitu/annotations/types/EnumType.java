package io.github.mhradek.aurkitu.annotations.types;

/**
 * The underlying integer type of an enum, set with {@code FlatBufferEnum.enumType} and written as
 * {@code enum Name : type}. Every value of the enum must fit it. {@code bit_flags} enums need an
 * unsigned type.
 *
 * @author m.hradek
 */
public enum EnumType {
    /** {@code byte}: 8-bit signed, -128 to 127. */
    BYTE(byte.class),
    /** {@code ubyte}: 8-bit unsigned, 0 to 255. */
    UBYTE(byte.class),
    /** {@code short}: 16-bit signed. */
    SHORT(short.class),
    /** {@code ushort}: 16-bit unsigned. */
    USHORT(short.class),
    /** {@code int}: 32-bit signed. */
    INT(int.class),
    /** {@code uint}: 32-bit unsigned. */
    UINT(int.class),
    /** {@code long}: 64-bit signed. */
    LONG(long.class),
    /** {@code ulong}: 64-bit unsigned. */
    ULONG(long.class);

    /** The Java type that holds this type's values (the signed type of the same width). */
    public Class<?> targetClass;

    EnumType(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
