package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

@FlatBufferEnum(enumType = EnumType.SHORT)
public enum SampleEnumShort {
    ALPHA, BETA, GAMMA
}
