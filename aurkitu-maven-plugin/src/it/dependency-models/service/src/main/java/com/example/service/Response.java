package com.example.service;

import com.example.models.CallType;
import com.example.models.Wallet;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

import java.util.List;

/** The service's own root type, built from types in the models jar. */
@FlatBufferTable(rootType = true)
public class Response {
    private CallType call;
    private List<Wallet> wallets;
}
