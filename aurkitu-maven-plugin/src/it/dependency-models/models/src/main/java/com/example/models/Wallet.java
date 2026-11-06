package com.example.models;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Lives in the models jar; its nested enum is declared automatically. */
@FlatBufferTable
public class Wallet {
    private long balance;
    private Currency currency;

    public enum Currency {
        AED, CZK, EUR, USD
    }
}
