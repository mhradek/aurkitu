package com.michaelhradek.aurkitu.test.service.payloads;

import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.test.dependency.LookupError;
import com.michaelhradek.aurkitu.test.dependency.Wallet;
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
