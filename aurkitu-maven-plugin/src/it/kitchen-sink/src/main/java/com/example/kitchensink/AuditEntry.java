package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Declared in its own namespace; references back to the default namespace are qualified. */
@FlatBufferTable(namespace = "com.example.kitchensink.audit")
public class AuditEntry {
    private String action;
    private long at;
    private Color color;
}
