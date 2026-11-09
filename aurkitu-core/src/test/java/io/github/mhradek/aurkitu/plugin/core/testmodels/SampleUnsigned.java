package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleUnsigned {

    @FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
    private byte flags;

    @FlatBufferFieldOptions(fieldType = FieldType.USHORT)
    private short port;

    @FlatBufferFieldOptions(fieldType = FieldType.UINT)
    private int count;

    @FlatBufferFieldOptions(fieldType = FieldType.ULONG)
    private long id;
}
