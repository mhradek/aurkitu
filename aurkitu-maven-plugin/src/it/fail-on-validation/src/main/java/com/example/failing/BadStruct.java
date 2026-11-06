package com.example.failing;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Structs may only contain scalars and other structs. */
@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT, rootType = true)
public class BadStruct {
    private int ok;
    private String notAllowed;
}
