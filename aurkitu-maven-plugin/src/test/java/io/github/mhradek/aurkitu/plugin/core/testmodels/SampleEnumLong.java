package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferEnum(enumType = FieldType.LONG)
public enum SampleEnumLong {
    A, B, C
}
