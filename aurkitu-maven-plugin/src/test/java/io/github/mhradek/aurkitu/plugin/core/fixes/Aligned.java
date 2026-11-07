package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT, forceAlign = 16)
public class Aligned {
    private float x;
    private Integer boxedInStruct;

    public float getX() {
        return x;
    }

    public Integer getBoxedInStruct() {
        return boxedInStruct;
    }
}
