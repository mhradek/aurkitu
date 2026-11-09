package io.github.mhradek.aurkitu.plugin.core.mapkeys;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** No 0 value: a map key or value of this type gets its lowest value as default. */
@FlatBufferEnum(enumType = EnumType.INT)
public enum Code {
    LOW(10), HIGH(20);

    @FlatBufferEnumTypeField
    private final int value;

    Code(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
