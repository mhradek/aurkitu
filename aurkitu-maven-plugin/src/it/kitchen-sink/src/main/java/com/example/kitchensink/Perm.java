package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

/** bit_flags: READ = 1, WRITE = 2, ADMIN = 4; 0 means no permissions. */
@FlatBufferEnum(enumType = EnumType.UBYTE, bitFlags = true)
public enum Perm {
    @FlatBufferComment(comment = "May read")
    READ,
    WRITE,
    ADMIN
}
