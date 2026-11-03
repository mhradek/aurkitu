package io.github.mhradek.aurkitu.test.dependency;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

@FlatBufferEnum
public enum CallType {

    UNKNOWN,
    WINDOWS,
    MAC_OC,
    IOS,
    ANDROID
}
