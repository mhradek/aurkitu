package com.michaelhradek.aurkitu.annotations;

import com.michaelhradek.aurkitu.annotations.types.EnumType;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;

public class FlatBufferEnumTest {

    @Test
    public void testDefault() {
        TestEnumDefault testEnumDefault = TestEnumDefault.VALUE_ONE;
        Annotation[] listAnnotations = testEnumDefault.getClass().getAnnotations();
        Assert.assertNotNull(listAnnotations);
        Assert.assertEquals(1, listAnnotations.length);

        Annotation classAnnotation = testEnumDefault.getClass().getAnnotation(FlatBufferEnum.class);
        Assert.assertNotNull(classAnnotation);

        Assert.assertEquals(EnumType.BYTE, ((FlatBufferEnum) classAnnotation).enumType());
        Assert.assertEquals(FlatBufferEnum.EnumStructureType.ENUM, ((FlatBufferEnum) classAnnotation).value());

        Assert.assertEquals("enum", ((FlatBufferEnum) classAnnotation).value().toString());
    }

    @Test
    public void testExtended() {
        TestEnumExtended testEnumExtended = TestEnumExtended.VALUE_TWO;
        Annotation[] listAnnotations = testEnumExtended.getClass().getAnnotations();
        Assert.assertNotNull(listAnnotations);
        Assert.assertEquals(2, listAnnotations.length);

        Annotation classAnnotation = testEnumExtended.getClass().getAnnotation(FlatBufferEnum.class);
        Assert.assertNotNull(classAnnotation);

        Assert.assertEquals(EnumType.ULONG, ((FlatBufferEnum) classAnnotation).enumType());
        Assert.assertEquals(FlatBufferEnum.EnumStructureType.UNION, ((FlatBufferEnum) classAnnotation).value());

        Assert.assertEquals("union", ((FlatBufferEnum) classAnnotation).value().toString());
    }

    @FlatBufferEnum
    private enum TestEnumDefault {

        VALUE_ONE, VALUE_TWO
    }

    @FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.UNION, enumType = EnumType.ULONG)
    @Deprecated
    private enum TestEnumExtended {

        VALUE_ONE, VALUE_TWO
    }
}