package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** Default underlying type (byte), implicit values. */
@FlatBufferComment(comment = "Primary colours.\nWritten as /// lines, one per line of text.")
@FlatBufferEnum
public enum Color {
    @FlatBufferComment(comment = "The default")
    RED,
    GREEN,
    BLUE
}
