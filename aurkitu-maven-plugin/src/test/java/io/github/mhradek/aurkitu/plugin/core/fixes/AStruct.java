package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Sorts before ZStruct alphabetically but contains it, so must be declared after it. */
@FlatBufferTable(FlatBufferTable.TableStructureType.STRUCT)
public class AStruct {
    private ZStruct inner;
    private int extra;
}
