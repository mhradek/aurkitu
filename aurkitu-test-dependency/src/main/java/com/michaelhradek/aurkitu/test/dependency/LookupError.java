package com.michaelhradek.aurkitu.test.dependency;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import lombok.Data;

@Data
@FlatBufferTable
public class LookupError {
    private int code;
    private String message;
}
