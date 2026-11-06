package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

@FlatBufferEnum(enumType = EnumType.BYTE)
public enum BadEnum {
    A(1), B(1), C(500);

    @FlatBufferEnumTypeField
    private final int code;

    BadEnum(int code) { this.code = code; }
}
