package io.github.mhradek.aurkitu.plugin.core.mapenums;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** bit_flags with no bit 0: 0 still means no flags, so no default is needed. */
@FlatBufferEnum(enumType = EnumType.UBYTE, bitFlags = true)
public enum Perm {
    READ(1), WRITE(2);

    @FlatBufferEnumTypeField
    private final byte bit;

    Perm(int bit) {
        this.bit = (byte) bit;
    }

    public byte getBit() {
        return bit;
    }
}
