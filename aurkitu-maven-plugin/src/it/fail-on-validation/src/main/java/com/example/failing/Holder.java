package com.example.failing;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(rootType = true)
public class Holder {
    private BadStruct value;
}
