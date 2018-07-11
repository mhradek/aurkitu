package com.michaelhradek.aurkitu.test.other;

import com.michaelhradek.aurkitu.annotations.FlatBufferComment;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.EnumType;
import com.michaelhradek.aurkitu.annotations.types.FieldType;

@FlatBufferTable
public class SampleAnonymousEnum {

    private enum Option {
        FirstOption,
        SecondOption,
        ThirdOption
    }

    Option option;

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

    VectorSize size;

    @FlatBufferEnum(enumType = EnumType.LONG)
    protected enum SplineEstimate {

        SMALL((long) 10000), MEDIUM((long) 20000), LARGE((long) 30000);

        @FlatBufferEnumTypeField
        long id;

        SplineEstimate(long id) {
            this.id = id;
        }
    }

    SplineEstimate estimate;

    @FlatBufferEnum(enumType = EnumType.INT)
    protected enum Matrix {

        SMALL(10000), MEDIUM(20000), LARGE(30000);

        @FlatBufferEnumTypeField
        int id;

        Matrix(int id) {
            this.id = id;
        }
    }

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SMALL")
    Matrix matrix;
}
