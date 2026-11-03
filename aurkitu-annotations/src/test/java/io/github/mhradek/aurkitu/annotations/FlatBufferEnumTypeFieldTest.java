package io.github.mhradek.aurkitu.annotations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FlatBufferEnumTypeFieldTest {

    @Test
    public void testDefault() throws NoSuchFieldException {
        TestEnumDefault testEnum = TestEnumDefault.VALUE;
        Annotation[] listClassAnnotations = testEnum.getClass().getAnnotations();
        assertNotNull(listClassAnnotations);
        assertEquals(1, listClassAnnotations.length);

        Field optionsField = testEnum.getClass().getField("enumValue");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        assertNotNull(listFieldAnnotations);
        assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferEnumTypeField.class);
        assertNotNull(fieldAnnotation);
    }

    @Test
    public void testExtended() throws NoSuchFieldException {
        TestEnumDefault testEnum = TestEnumDefault.VALUE;
        Annotation[] listClassAnnotations = testEnum.getClass().getAnnotations();
        assertNotNull(listClassAnnotations);
        assertEquals(1, listClassAnnotations.length);

        Field optionsField = testEnum.getClass().getField("enumName");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        assertNotNull(listFieldAnnotations);
        assertEquals(2, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferEnumTypeField.class);
        assertNotNull(fieldAnnotation);
    }


    @FlatBufferEnum
    private enum TestEnumDefault {

        VALUE(1, "test");

        @FlatBufferEnumTypeField
        public int enumValue;

        @FlatBufferEnumTypeField
        @Deprecated
        public String enumName;

        TestEnumDefault(int value, String name) {
            this.enumValue = value;
            this.enumName = name;
        }
    }
}