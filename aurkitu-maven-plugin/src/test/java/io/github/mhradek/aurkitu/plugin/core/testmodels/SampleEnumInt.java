package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferEnum(enumType = FieldType.INT)
public enum SampleEnumInt {
    ONE, TWO, THREE
}
