package com.michaelhradek.aurkitu.test.service.payloads;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import lombok.Data;

@Data
@FlatBufferTable
public class Response {
    String userId;
    String username;
    long createDate;
}
