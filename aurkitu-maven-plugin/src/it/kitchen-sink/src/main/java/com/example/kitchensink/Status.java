package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** Unsigned underlying type with explicit, non-contiguous values. */
@FlatBufferEnum(enumType = EnumType.UINT)
public enum Status {
    ACTIVE(1), SUSPENDED(5), CLOSED(10);

    @FlatBufferEnumTypeField
    private final int code;

    Status(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
