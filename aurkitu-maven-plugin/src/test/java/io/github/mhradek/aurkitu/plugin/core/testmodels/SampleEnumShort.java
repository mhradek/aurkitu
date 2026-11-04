package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferEnum(enumType = FieldType.SHORT)
public enum SampleEnumShort {
    ALPHA, BETA, GAMMA
}
