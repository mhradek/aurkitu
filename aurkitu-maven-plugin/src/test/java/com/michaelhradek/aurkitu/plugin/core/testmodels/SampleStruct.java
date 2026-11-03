package com.michaelhradek.aurkitu.plugin.core.testmodels;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class SampleStruct {
    private float x;
    private float y;
    private float z;
}
