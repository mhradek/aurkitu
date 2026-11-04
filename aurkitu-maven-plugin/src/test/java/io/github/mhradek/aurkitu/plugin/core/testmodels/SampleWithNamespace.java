package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(namespace = "other.ns")
public class SampleWithNamespace {
    private int value;
}
