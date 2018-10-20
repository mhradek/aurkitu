package com.michaelhradek.aurkitu.test.service.payloads;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.test.dependency.Wallet;
import lombok.Data;

@Data
@FlatBufferTable
public class Response {
    String userId;
    String username;
    long createDate;
    Wallet wallet;
}
