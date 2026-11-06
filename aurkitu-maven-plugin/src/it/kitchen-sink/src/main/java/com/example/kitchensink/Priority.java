package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** Declared out of value order; emitted in ascending order. */
@FlatBufferEnum(enumType = EnumType.SHORT)
public enum Priority {
    HIGH((short) 30), LOW((short) 10), MEDIUM((short) 20);

    @FlatBufferEnumTypeField
    private final short weight;

    Priority(short weight) {
        this.weight = weight;
    }
}
