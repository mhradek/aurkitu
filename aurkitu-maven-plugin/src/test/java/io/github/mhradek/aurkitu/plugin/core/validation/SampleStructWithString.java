package io.github.mhradek.aurkitu.plugin.core.validation;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class SampleStructWithString {
    private String name;
    private int value;
}
