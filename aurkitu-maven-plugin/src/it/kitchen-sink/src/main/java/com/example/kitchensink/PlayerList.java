package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** A user type whose name ends in "List": must be referenced, not treated as a collection. */
@FlatBufferTable
public class PlayerList {
    private int count;
}
