package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

@FlatBufferEnum
@FlatBufferComment(comment = "A commented enum")
public enum SampleEnumWithComment {
    YES, NO
}
