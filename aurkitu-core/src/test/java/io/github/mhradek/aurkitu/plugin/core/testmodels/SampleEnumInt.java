package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

@FlatBufferEnum(enumType = EnumType.INT)
public enum SampleEnumInt {
    ONE, TWO, THREE
}
