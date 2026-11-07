package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(rootType = true)
public class Root {
    private Level level;
    private Note note;
    // Fully qualified on purpose: Note in this package has the same simple name.
    @SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
    private io.github.mhradek.aurkitu.plugin.core.namespaces.other.Note otherNote;
    private Mixed mixed;
}
