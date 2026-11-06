package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** Only one @FlatBufferEnumTypeField is allowed. */
@FlatBufferEnum(enumType = EnumType.INT)
public enum TwoValueFields {
    A(1, 2);

    @FlatBufferEnumTypeField
    private final int first;

    @FlatBufferEnumTypeField
    private final int second;

    TwoValueFields(int first, int second) {
        this.first = first;
        this.second = second;
    }

    public int getFirst() {
        return first;
    }

    public int getSecond() {
        return second;
    }
}
