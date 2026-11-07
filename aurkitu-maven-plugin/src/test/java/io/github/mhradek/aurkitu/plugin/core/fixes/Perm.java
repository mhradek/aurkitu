package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

@FlatBufferComment(comment = "Permissions\nOne bit each")
@FlatBufferEnum(enumType = EnumType.UBYTE, bitFlags = true, attributes = {"priority: 2"})
public enum Perm {
    @FlatBufferComment(comment = "May read")
    READ,
    WRITE
}
