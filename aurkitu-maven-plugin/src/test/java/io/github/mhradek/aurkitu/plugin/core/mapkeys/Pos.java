package io.github.mhradek.aurkitu.plugin.core.mapkeys;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class Pos {
    private int x;

    public int getX() {
        return x;
    }
}
