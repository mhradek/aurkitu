package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT, forceAlign = 16)
public class Vec3 {
    private float x;
    private float y;
    private float z;
}
