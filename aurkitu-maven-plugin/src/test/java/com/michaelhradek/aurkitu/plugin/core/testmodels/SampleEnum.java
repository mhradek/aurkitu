package com.michaelhradek.aurkitu.plugin.core.testmodels;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.types.EnumType;

@FlatBufferEnum(enumType = EnumType.BYTE)
public enum SampleEnum {
    ALPHA, BETA, GAMMA
}
