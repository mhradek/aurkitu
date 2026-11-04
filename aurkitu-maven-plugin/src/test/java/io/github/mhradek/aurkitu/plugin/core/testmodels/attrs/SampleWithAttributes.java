package io.github.mhradek.aurkitu.plugin.core.testmodels.attrs;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;
import io.github.mhradek.aurkitu.plugin.core.testmodels.SampleEnum;

@FlatBufferTable
public class SampleWithAttributes {

    @FlatBufferFieldOptions(fieldType = FieldType.STRING, fieldId = 0, key = true)
    private String id;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 1, hash = "fnv1_32")
    private int hashField;

    @FlatBufferFieldOptions(fieldType = FieldType.UBYTE, nestedFlatbuffer = "Nested")
    private byte[] nested;

    @FlatBufferFieldOptions(fieldType = FieldType.UBYTE, flexbuffer = true)
    private byte[] flex;

    @FlatBufferFieldOptions(fieldType = FieldType.INT, forceAlign = 8)
    private int aligned;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    private SampleEnum fullNameEnum;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT)
    private FixedStruct fixedStruct;
}

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
class FixedStruct {
    @FlatBufferFieldOptions(fieldType = FieldType.INT, fixedLength = 4)
    private int[] values;
}
