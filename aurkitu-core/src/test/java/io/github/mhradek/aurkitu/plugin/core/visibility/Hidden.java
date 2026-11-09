package io.github.mhradek.aurkitu.plugin.core.visibility;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** A package-private annotated table. */
@FlatBufferTable
class Hidden {
    int secret;
}
