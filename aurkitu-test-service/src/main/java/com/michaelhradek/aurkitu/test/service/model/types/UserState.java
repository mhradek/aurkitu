package com.michaelhradek.aurkitu.test.service.model.types;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;

@FlatBufferEnum
public enum UserState {
    GUEST,
    ACTIVE,
    DISABLED,
    INACTIVE;
}
