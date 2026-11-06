package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(FlatBufferTable.TableStructureType.STRUCT)
public class Vec3 {
    private float x;
    private float y;
    private float z;
}
