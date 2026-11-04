package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleWithPartialIds {
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, fieldId = 0)
    private String id;
    private String name;
}
