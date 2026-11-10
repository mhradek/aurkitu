package com.example.game;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class Item {

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, required = true)
    private String name;

    private int quantity;

    /** Rarity has no value 0, so the field needs a default that is one of its values. */
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "COMMON")
    private Rarity rarity;
}
