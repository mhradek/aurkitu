package io.github.mhradek.aurkitu.test.service.model.types;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

@FlatBufferEnum
public enum UserState {
    GUEST,
    ACTIVE,
    DISABLED,
    INACTIVE;
}
