package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

@FlatBufferEnum(enumType = EnumType.BYTE)
public enum SampleEnum {
    ALPHA, BETA, GAMMA
}
