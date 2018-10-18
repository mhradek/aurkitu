package com.michaelhradek.aurkitu.test.service.interfaces;

interface Protocol<T> {

    byte encode(T payload);

    T decode(byte payload);
}
