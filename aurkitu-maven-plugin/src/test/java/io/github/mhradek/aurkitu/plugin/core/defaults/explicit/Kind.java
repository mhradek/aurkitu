package io.github.mhradek.aurkitu.plugin.core.defaults.explicit;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.types.EnumType;

@FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.ENUM, enumType = EnumType.BYTE, namespace = "",
        bitFlags = false, attributes = {})
public enum Kind { A, B }
