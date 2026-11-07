package io.github.mhradek.aurkitu.plugin.core.warnings;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** A valid schema with one warning only: inherited fields without ids. */
@FlatBufferTable(rootType = true)
public class WarnChild extends WarnBase {
    private int own;
}
