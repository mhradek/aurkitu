package com.example.game;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** A union: a field of this type holds one of these tables. */
@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)
public enum Event {
    LevelUp,
    ItemFound
}
