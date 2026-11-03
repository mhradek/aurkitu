package com.michaelhradek.aurkitu.plugin.core.testmodels;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.types.FieldType;

@FlatBufferEnum(enumType = FieldType.BYTE)
public enum SampleEnum {
    ALPHA, BETA, GAMMA
}
