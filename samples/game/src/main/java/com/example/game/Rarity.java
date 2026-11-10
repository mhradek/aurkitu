package com.example.game;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** Explicit values, from the field marked @FlatBufferEnumTypeField. None is 0, so fields of this type need a default. */
@FlatBufferEnum(enumType = EnumType.UBYTE)
public enum Rarity {
    COMMON((byte) 1),
    RARE((byte) 5),
    LEGENDARY((byte) 10);

    @FlatBufferEnumTypeField
    private final byte weight;

    Rarity(byte weight) {
        this.weight = weight;
    }
}
