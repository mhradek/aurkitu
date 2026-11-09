package io.github.mhradek.aurkitu.plugin.core.options;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** optionalScalars never applies in a struct. */
@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class Point {
    private Integer x;
    private int y;

    public Integer getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
