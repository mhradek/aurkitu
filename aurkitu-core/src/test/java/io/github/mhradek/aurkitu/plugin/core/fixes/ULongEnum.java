package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** -1L is 18446744073709551615 as a ulong, so it sorts after 1. */
@FlatBufferEnum(enumType = EnumType.ULONG)
public enum ULongEnum {
    MAX(-1L), ONE(1L);

    @FlatBufferEnumTypeField
    private final long code;

    ULongEnum(long code) { this.code = code; }

    public long getCode() { return code; }
}
