package com.example.game;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** A struct: fixed size, stored inline, for small values that are always present. */
@FlatBufferTable(FlatBufferTable.TableStructureType.STRUCT)
public class Vec3 {
    private float x;
    private float y;
    private float z;
}
