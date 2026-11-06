package io.github.mhradek.aurkitu.plugin.core.model;

import java.math.BigInteger;

/**
 * FlatBuffers scalar types. Integral kinds carry their value range (used for enum values).
 */
public enum ScalarKind {
    BOOL(null, null),
    BYTE(Byte.MIN_VALUE, Byte.MAX_VALUE),
    UBYTE(0, 0xFF),
    SHORT(Short.MIN_VALUE, Short.MAX_VALUE),
    USHORT(0, 0xFFFF),
    INT(Integer.MIN_VALUE, Integer.MAX_VALUE),
    UINT(0, 0xFFFF_FFFFL),
    LONG(Long.MIN_VALUE, Long.MAX_VALUE),
    ULONG(BigInteger.ZERO, BigInteger.TWO.pow(64).subtract(BigInteger.ONE)),
    FLOAT(null, null),
    DOUBLE(null, null);

    private final BigInteger min;
    private final BigInteger max;

    ScalarKind(long min, long max) {
        this(BigInteger.valueOf(min), BigInteger.valueOf(max));
    }

    ScalarKind(BigInteger min, BigInteger max) {
        this.min = min;
        this.max = max;
    }

    /** The schema keyword, e.g. {@code uint}. */
    public String keyword() {
        return name().toLowerCase();
    }

    public boolean isIntegral() {
        return min != null;
    }

    public boolean isUnsigned() {
        return isIntegral() && min.signum() == 0;
    }

    /** True if {@code value} fits this integral kind. */
    public boolean contains(BigInteger value) {
        return isIntegral() && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
