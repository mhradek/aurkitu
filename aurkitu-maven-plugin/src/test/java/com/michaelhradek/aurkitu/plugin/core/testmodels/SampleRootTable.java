package com.michaelhradek.aurkitu.plugin.core.testmodels;

import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;

import java.util.List;
import java.util.Map;

@FlatBufferTable(rootType = true)
public class SampleRootTable {

    private long id;
    private String name;
    private boolean active;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, defaultValue = "42")
    private int level;

    @FlatBufferIgnore
    private String ignored;

    private List<String> tags;
    private Map<String, Integer> scores;
    private Map<String, Map<String, Boolean>> nestedMap;
    private SampleEnum status;
}
