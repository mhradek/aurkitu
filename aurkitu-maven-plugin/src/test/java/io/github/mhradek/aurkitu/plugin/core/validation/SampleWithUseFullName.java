package io.github.mhradek.aurkitu.plugin.core.validation;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleWithUseFullName {
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    private SampleWithFailOnIssues nested;
}
