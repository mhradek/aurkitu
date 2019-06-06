package com.michaelhradek.aurkitu.plugin.test.other;

import com.michaelhradek.aurkitu.annotations.*;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleAnonymousEnum {

    public Option option;
    public VectorSize size;
    public SplineEstimate estimate;
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SMALL")
    public Matrix matrix;

    private enum Option {
        FirstOption,
        SecondOption,
        ThirdOption
    }

    @FlatBufferComment(comment = "This is a enum comment")
    @FlatBufferEnum(enumType = EnumType.SHORT)
    protected enum VectorSize {

        SMALL((short) 10000), MEDIUM((short) 20000), LARGE((short) 30000);

        @FlatBufferEnumTypeField
        short id;

        VectorSize(short id) {
            this.id = id;
        }
    }

    @FlatBufferEnum(enumType = EnumType.LONG)
    protected enum SplineEstimate {

        SMALL((long) 10000), MEDIUM((long) 20000), LARGE((long) 30000);

        @FlatBufferEnumTypeField
        long id;

        SplineEstimate(long id) {
            this.id = id;
        }
    }

    @FlatBufferEnum(enumType = EnumType.INT)
    protected enum Matrix {

        SMALL(10000), MEDIUM(20000), LARGE(30000);

        @FlatBufferEnumTypeField
        int id;

        Matrix(int id) {
            this.id = id;
        }
    }
}
