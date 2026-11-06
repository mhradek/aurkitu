package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** Union of the Circle and Square tables. */
@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)
public enum Shape {
    Circle, Square
}
