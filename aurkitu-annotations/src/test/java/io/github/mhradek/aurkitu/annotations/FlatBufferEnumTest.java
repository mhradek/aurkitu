package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.EnumType;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

public class FlatBufferEnumTest {

    @Test
    public void testDefault() {
        TestEnumDefault testEnumDefault = TestEnumDefault.VALUE_ONE;
        Annotation[] listAnnotations = testEnumDefault.getClass().getAnnotations();
        assertNotNull(listAnnotations);
        assertEquals(1, listAnnotations.length);

        Annotation classAnnotation = testEnumDefault.getClass().getAnnotation(FlatBufferEnum.class);
        assertNotNull(classAnnotation);

        assertEquals(EnumType.BYTE, ((FlatBufferEnum) classAnnotation).enumType());
        assertEquals(FlatBufferEnum.EnumStructureType.ENUM, ((FlatBufferEnum) classAnnotation).value());

        assertEquals("enum", ((FlatBufferEnum) classAnnotation).value().toString());
    }

    @Test
    public void testExtended() {
        TestEnumExtended testEnumExtended = TestEnumExtended.VALUE_TWO;
        Annotation[] listAnnotations = testEnumExtended.getClass().getAnnotations();
        assertNotNull(listAnnotations);
        assertEquals(2, listAnnotations.length);

        Annotation classAnnotation = testEnumExtended.getClass().getAnnotation(FlatBufferEnum.class);
        assertNotNull(classAnnotation);

        assertEquals(EnumType.ULONG, ((FlatBufferEnum) classAnnotation).enumType());
        assertEquals(FlatBufferEnum.EnumStructureType.UNION, ((FlatBufferEnum) classAnnotation).value());

        assertEquals("union", ((FlatBufferEnum) classAnnotation).value().toString());
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