package com.example.game;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.util.List;
import java.util.Map;

/** The root of every buffer: a player and everything a client shows about them. */
@FlatBufferComment(comment = "A player and what a client shows about them.")
@FlatBufferTable(rootType = true)
public class Player {

    private long id;

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, required = true)
    private String displayName;

    @FlatBufferComment(comment = "Starts at 1.")
    @FlatBufferFieldOptions(fieldType = FieldType.INT, defaultValue = "1")
    private int level;

    private Status status;

    private Vec3 position;

    private List<Item> inventory;

    @FlatBufferComment(comment = "Free-form counters, e.g. \"wins\".")
    private Map<String, Integer> stats;

    @FlatBufferComment(comment = "What happened to the player most recently.")
    private Event lastEvent;

    /** Server-side only: never sent to clients, so not in the schema. */
    @FlatBufferIgnore
    private String sessionToken;
}
