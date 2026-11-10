package com.example.game;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** Implicit values: OFFLINE is 0, ONLINE 1, IN_MATCH 2, in declaration order. */
@FlatBufferEnum
public enum Status {
    OFFLINE,
    ONLINE,
    @FlatBufferComment(comment = "Playing a match right now.")
    IN_MATCH
}
