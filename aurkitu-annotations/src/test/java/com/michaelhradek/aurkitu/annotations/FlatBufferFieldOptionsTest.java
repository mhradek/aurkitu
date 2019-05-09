package com.michaelhradek.aurkitu.annotations;

import com.michaelhradek.aurkitu.annotations.types.FieldType;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FlatBufferFieldOptionsTest {

    private final static String TEST_DEFAULT_VALUE = "testDefaultValueForField";

    @Test
    public void testDefaults() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(1, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("optionsField");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);

        Assert.assertEquals(FieldType.STRING, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());
        Assert.assertEquals(false, ((FlatBufferFieldOptions) fieldAnnotation).useFullName());
        Assert.assertEquals("", ((FlatBufferFieldOptions) fieldAnnotation).defaultValue());
    }

    @Test
    public void testExtended() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(1, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("optionsFieldExtended");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(2, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);

        Assert.assertEquals(FieldType.ARRAY, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());
        Assert.assertEquals(true, ((FlatBufferFieldOptions) fieldAnnotation).useFullName());
        Assert.assertEquals(TEST_DEFAULT_VALUE, ((FlatBufferFieldOptions) fieldAnnotation).defaultValue());
    }

    @FlatBufferTable
    private class TestTableDefault {

        @FlatBufferFieldOptions(fieldType = FieldType.STRING)
        public String optionsField;

        @FlatBufferFieldOptions(fieldType = FieldType.ARRAY, useFullName = true, defaultValue = TEST_DEFAULT_VALUE)
        @Deprecated
        public String[] optionsFieldExtended;
    }
}