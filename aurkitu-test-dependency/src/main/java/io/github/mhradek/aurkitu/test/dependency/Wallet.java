package io.github.mhradek.aurkitu.test.dependency;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import lombok.Data;

@Data
@FlatBufferTable
public class Wallet {
    String userId;
    long balance;
    long lastUpdate;

}
