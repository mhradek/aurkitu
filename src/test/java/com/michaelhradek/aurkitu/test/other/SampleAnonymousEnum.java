package com.michaelhradek.aurkitu.test.other;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferEnumTypeField;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.FieldType;

@FlatBufferTable
public class SampleAnonymousEnum {

    private enum Option {
        FirstOption,
        SecondOption,
        ThirdOption
    }

    Option option;

    @FlatBufferEnum(enumType = FieldType.SHORT)
    protected enum VectorSize {

        SMALL((short) 10000), MEDIUM((short) 20000), LARGE((short) 30000);

        @FlatBufferEnumTypeField
        short id;

        VectorSize(short id) {
            this.id = id;
        }
    }

    VectorSize size;

    @FlatBufferEnum(enumType = FieldType.LONG)
    protected enum SplineEstimate {

        SMALL((long) 10000), MEDIUM((long) 20000), LARGE((long) 30000);

        @FlatBufferEnumTypeField
        long id;

        SplineEstimate(long id) {
            this.id = id;
        }
    }

    SplineEstimate estimate;

    @FlatBufferEnum(enumType = FieldType.INT)
    protected enum Matrix {

        SMALL(10000), MEDIUM(20000), LARGE(30000);

        @FlatBufferEnumTypeField
        int id;

        Matrix(int id) {
            this.id = id;
        }
    }

    Matrix matrix;

    @FlatBufferEnum(enumType = FieldType.FLOAT)
    protected enum Impulse {

        SMALL(123f), MEDIUM(456f), LARGE(789f);

        @FlatBufferEnumTypeField
        float id;

        Impulse(float id) {
            this.id = id;
        }
    }

    Impulse impulse;

    @FlatBufferEnum(enumType = FieldType.DOUBLE)
    protected enum Factor {

        SMALL(10000d), MEDIUM(20000d), LARGE(30000d);

        @FlatBufferEnumTypeField
        double id;

        Factor(double id) {
            this.id = id;
        }
    }

    Factor factor;
}
