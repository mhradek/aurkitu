package io.github.mhradek.aurkitu.test.service.payloads;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.test.dependency.CallType;
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
