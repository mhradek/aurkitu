package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

/** (byte) 0xFF is -1 in Java but 255 as a ubyte. */
@FlatBufferEnum(enumType = EnumType.UBYTE)
public enum ByteCodedEnum {
    MAX((byte) 0xFF), ONE((byte) 1);

    @FlatBufferEnumTypeField
    private final byte code;

    ByteCodedEnum(byte code) { this.code = code; }
}
