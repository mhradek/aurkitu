package io.github.mhradek.aurkitu.plugin.core.attrs;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class FixedStruct {
    @FlatBufferFieldOptions(fieldType = FieldType.INT, fixedLength = 4)
    private int[] values;
}
