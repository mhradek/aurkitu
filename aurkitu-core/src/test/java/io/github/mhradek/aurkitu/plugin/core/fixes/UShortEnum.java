package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** (short) 0xFFFF is -1 in Java but 65535 as a ushort. */
@FlatBufferEnum(enumType = EnumType.USHORT)
public enum UShortEnum {
    MAX((short) 0xFFFF);

    @FlatBufferEnumTypeField
    private final short code;

    UShortEnum(short code) { this.code = code; }

    public short getCode() { return code; }
}
