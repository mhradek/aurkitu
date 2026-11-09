package io.github.mhradek.aurkitu.plugin.core.options;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

/** Ids on every field, so a static field without one (ignoreStaticMembers=false) makes them partial. */
@FlatBufferTable
public class Ledger {
    @FlatBufferFieldOptions(fieldType = FieldType.LONG, fieldId = 0)
    private long entryId;
    @FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 1)
    private int amount;
    static int openCount;

    public long getEntryId() {
        return entryId;
    }

    public int getAmount() {
        return amount;
    }
}
