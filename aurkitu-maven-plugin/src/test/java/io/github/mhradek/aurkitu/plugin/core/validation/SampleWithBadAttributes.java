package io.github.mhradek.aurkitu.plugin.core.validation;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleWithBadAttributes {
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, nestedFlatbuffer = "Nested")
    private String notBinary;
    @FlatBufferFieldOptions(fieldType = FieldType.INT, flexbuffer = true)
    private int alsoNotBinary;
}
