package io.github.mhradek.aurkitu.plugin.core.attrs;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Root type of the nested_flatbuffer field in SampleWithAttributes. */
@FlatBufferTable
public class Nested {
    private int value;

    public int getValue() {
        return value;
    }
}
