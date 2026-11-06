package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

/** Declared out of order; emitted sorted by code. */
@FlatBufferEnum(enumType = EnumType.SHORT)
public enum CodedEnum {
    HIGH((short) 30), LOW((short) -10), MEDIUM((short) 20);

    @FlatBufferEnumTypeField
    private final short code;

    CodedEnum(short code) { this.code = code; }

    public short getCode() { return code; }
}
