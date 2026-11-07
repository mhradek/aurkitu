package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferUnionMember;

/** Union of the Circle and Square tables; Box is an alias for Square. */
@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)
public enum Shape {
    Circle,
    Square,
    @FlatBufferComment(comment = "A square under another name")
    @FlatBufferUnionMember(Square.class)
    Box
}
