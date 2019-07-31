package com.michaelhradek.aurkitu.test.service.payloads;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.test.dependency.CallType;
import lombok.Data;

import java.util.Map;

@Data
@FlatBufferTable
public class Request {
    CallType callType;
    String userId;
    Float[] codes;
    Map<String, Integer> weightMap;
}
