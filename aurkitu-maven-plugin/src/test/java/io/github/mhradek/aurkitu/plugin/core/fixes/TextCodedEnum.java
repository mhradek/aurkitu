package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** A value field must be integral. */
@FlatBufferEnum(enumType = EnumType.INT)
public enum TextCodedEnum {
    A("a");

    @FlatBufferEnumTypeField
    private final String code;

    TextCodedEnum(String code) { this.code = code; }

    public String getCode() { return code; }
}
