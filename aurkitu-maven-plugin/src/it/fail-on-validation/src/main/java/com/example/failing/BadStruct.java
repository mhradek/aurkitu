package com.example.failing;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Structs may only contain scalars and other structs. */
@FlatBufferTable(FlatBufferTable.TableStructureType.STRUCT)
public class BadStruct {
    private int ok;
    private String notAllowed;
}
