package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleWithAttributes {

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, fieldId = 0, key = true)
    private String id;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 1, hash = "fnv1_32")
    private int hashField;

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, nestedFlatbuffer = "Nested")
    private String nested;

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, flexbuffer = true)
    private String flex;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, forceAlign = 8)
    private int aligned;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, fixedLength = 4)
    private int[] fixed;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    private SampleEnum fullNameEnum;
}
