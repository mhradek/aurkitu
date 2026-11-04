package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleWithAllIds {
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, fieldId = 0)
    private String id;
    @FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 1)
    private int version;
}
