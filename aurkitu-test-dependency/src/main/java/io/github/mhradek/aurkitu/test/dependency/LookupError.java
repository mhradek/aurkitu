package io.github.mhradek.aurkitu.test.dependency;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import lombok.Data;

@Data
@FlatBufferTable
public class LookupError {
    private int code;
    private String message;
}
