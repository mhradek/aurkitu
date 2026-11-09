package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Structs cannot contain tables. */
@FlatBufferTable(FlatBufferTable.TableStructureType.STRUCT)
public class StructWithTable {
    private int ok;
    private PlayerList players;
}
