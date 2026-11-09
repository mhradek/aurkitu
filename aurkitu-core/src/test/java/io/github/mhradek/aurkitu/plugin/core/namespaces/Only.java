package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** The only type with this name, in another namespace. */
@FlatBufferTable(namespace = "other")
public class Only {
    private int value;
}
