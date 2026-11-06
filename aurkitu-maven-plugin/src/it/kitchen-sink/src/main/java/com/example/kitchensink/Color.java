package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** Default underlying type (byte), implicit values. */
@FlatBufferComment(comment = "Primary colours")
@FlatBufferEnum
public enum Color {
    RED, GREEN, BLUE
}
