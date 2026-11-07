package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

/** Keeps its field order in the buffer; carries a custom attribute. */
@FlatBufferTable(originalOrder = true, attributes = {"help: \"An inventory item\""})
public class Item {
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, key = true)
    private String name;

    private int quantity;
}
