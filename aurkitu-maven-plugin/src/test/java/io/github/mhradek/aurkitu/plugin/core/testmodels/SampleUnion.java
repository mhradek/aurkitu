package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

@FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.UNION)
public enum SampleUnion {
    NONE,
    SampleRootTable,
    SampleStruct
}
