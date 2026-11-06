package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

/** Every field has an explicit id, so ids are emitted. */
@FlatBufferTable
public class Versioned {
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, fieldId = 1)
    private String label;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 0)
    private int revision;
}
