package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable
public class SampleWithPartialIds {
    @FlatBufferFieldOptions(fieldId = 0)
    private String id;
    private String name;
}
