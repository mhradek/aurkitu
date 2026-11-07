package io.github.mhradek.aurkitu.plugin.core.defaults.implicit;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

/** Every annotation element left at its default; see {@code explicit.Thing}. */
@FlatBufferTable
public class Thing {
    @FlatBufferFieldOptions(fieldType = FieldType.INT)
    private int count;
    private Integer boxed;
    private Kind kind;
    private Shape shape;
}
