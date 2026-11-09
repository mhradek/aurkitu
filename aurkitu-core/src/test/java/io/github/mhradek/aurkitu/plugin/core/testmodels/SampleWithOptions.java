package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
@FlatBufferComment(comment = "A table with various field options")
public class SampleWithOptions extends BaseEntity {

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, required = true)
    @FlatBufferComment(comment = "Required identifier")
    private String code;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, deprecated = true, defaultValue = "0")
    private int legacyValue;
}
