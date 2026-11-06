package io.github.mhradek.aurkitu.annotations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@SuppressWarnings("removal") // covers a deprecated annotation
public class FlatBufferOptionsTest {

    @Test
    public void testDefaults() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        assertNotNull(listClassAnnotations);
        assertEquals(2, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("ignoredField");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        assertNotNull(listFieldAnnotations);
        assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferOptions.class);
        assertNotNull(fieldAnnotation);
    }

    @FlatBufferTable
    @FlatBufferOptions
    private class TestTableDefault {

        @FlatBufferOptions
        public String ignoredField;

        @SuppressWarnings("unused")
        public String notIgnoredField;
    }
}