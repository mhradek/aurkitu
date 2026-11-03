package io.github.mhradek.aurkitu.test.service.payloads;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;
import io.github.mhradek.aurkitu.test.dependency.LookupError;
import io.github.mhradek.aurkitu.test.dependency.Wallet;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@FlatBufferTable
public class Response {

    public static String IGNORED_STATIC_FIELD = "ignoredStaticField";

    String userId;
    String username;
    @FlatBufferFieldOptions(fieldType = FieldType.ULONG)
    long createDate;
    Wallet wallet;
    List<LookupError> errors;
    List<Integer> codes;
    List<String> messages;
    Map<String, Character> dataMap;
    Map<String, List<String>> keys;
}
