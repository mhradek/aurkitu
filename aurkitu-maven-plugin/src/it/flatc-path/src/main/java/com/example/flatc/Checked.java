package com.example.flatc;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

import java.util.List;

@FlatBufferTable(rootType = true)
public class Checked {
    private String name;
    private List<Integer> values;
}
