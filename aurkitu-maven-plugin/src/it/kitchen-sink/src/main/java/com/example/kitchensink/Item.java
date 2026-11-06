package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class Item {
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, key = true)
    private String name;

    private int quantity;
}
