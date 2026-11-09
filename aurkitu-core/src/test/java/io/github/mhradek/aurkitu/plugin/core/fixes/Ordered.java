package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

@FlatBufferTable(originalOrder = true, attributes = {"priority: 1"})
public class Ordered {
    @FlatBufferFieldOptions(fieldType = FieldType.INT, attributes = {"priority: 3", "help: \"count\""})
    private int count;

    public int getCount() {
        return count;
    }
}
