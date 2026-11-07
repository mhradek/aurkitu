package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(rootType = true)
public class Root {
    private Level level;
    private Note note;
    private io.github.mhradek.aurkitu.plugin.core.namespaces.other.Note otherNote;
    private Mixed mixed;
}
