package com.michaelhradek.aurkitu.test.dependency;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import lombok.Data;

@Data
@FlatBufferTable
public class Wallet {
    String userId;
    long balance;
    long lastUpdate;
}
