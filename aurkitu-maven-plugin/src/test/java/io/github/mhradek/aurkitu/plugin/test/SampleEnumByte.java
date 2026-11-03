package io.github.mhradek.aurkitu.plugin.test;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

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
