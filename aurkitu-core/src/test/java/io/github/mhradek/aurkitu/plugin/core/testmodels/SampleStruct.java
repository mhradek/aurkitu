package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class SampleStruct {
    private float x;
    private float y;
    private float z;
}
