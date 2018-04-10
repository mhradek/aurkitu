package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import com.michaelhradek.aurkitu.core.output.EnumType;

/**
 * @author m.hradek
 */
@FlatBufferEnum(enumType = EnumType.BYTE)
public enum SampleEnumByte {
    EnvironmentAlpha((byte) 1), EnvironmentBeta((byte) 2), EnvironmentGamma((byte) 3);

    @FlatBufferEnumTypeField
    byte id;

    SampleEnumByte(byte id) {
        this.id = id;
    }
}
