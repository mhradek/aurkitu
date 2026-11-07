package io.github.mhradek.aurkitu.plugin.core.options;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** In its own namespace: qualified from the default one, unless the default is "audit" too. */
@FlatBufferTable(namespace = "audit")
public class Audit {
    private long at;

    public long getAt() {
        return at;
    }
}
