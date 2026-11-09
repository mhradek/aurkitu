package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** -1 is 4294967295 as a uint. */
@FlatBufferEnum(enumType = EnumType.UINT)
public enum UIntEnum {
    MAX(-1);

    @FlatBufferEnumTypeField
    private final int code;

    UIntEnum(int code) { this.code = code; }

    public int getCode() { return code; }
}
