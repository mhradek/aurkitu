package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** Its constants cannot be created, so values cannot be read. */
@FlatBufferEnum(enumType = EnumType.INT)
public enum FailingEnum {
    A(1);

    @FlatBufferEnumTypeField
    private final int code;

    FailingEnum(int code) {
        if (code > 0) {
            throw new IllegalStateException("cannot initialise " + code);
        }
        this.code = code;
    }

    public int getCode() { return code; }
}
