package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

@FlatBufferTable
public class Checkpoint {
    private int at;

    public int getAt() {
        return at;
    }
}
