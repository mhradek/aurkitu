package io.github.mhradek.aurkitu.annotations;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FlatBufferIgnoreTest {

    @Test
    public void testFlatbufferTableDefault() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        assertNotNull(listClassAnnotations);
        assertEquals(1, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("ignoredField");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        assertNotNull(listFieldAnnotations);
        assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferIgnore.class);
        assertNotNull(fieldAnnotation);
    }

    @Test
    public void testFlatbufferTableExtended() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        assertNotNull(listClassAnnotations);
        assertEquals(1, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("notIgnoredField");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        assertNotNull(listFieldAnnotations);
        assertEquals(0, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferIgnore.class);
        assertNull(fieldAnnotation);
    }

    @FlatBufferTable
    private class TestTableDefault {

        @FlatBufferIgnore
        public String ignoredField;

        @SuppressWarnings("unused")
        public String notIgnoredField;
    }
}