package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

/** Aliases: one table under two names; a plain member resolved by name. */
@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)
public enum Pos {
    @FlatBufferComment(comment = "Where it starts")
    @FlatBufferUnionMember(Checkpoint.class)
    Start,
    @FlatBufferUnionMember(Checkpoint.class)
    Finish,
    @FlatBufferUnionMember(Checkpoint.class)
    Checkpoint,
    PlayerList
}
