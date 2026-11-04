package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable
public class SampleWithAllIds {
    @FlatBufferFieldOptions(fieldId = 0)
    private String id;
    @FlatBufferFieldOptions(fieldId = 1)
    private int version;
}
