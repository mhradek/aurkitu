package com.michaelhradek.aurkitu.annotations;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FlatBufferOptionsTest {

    @Test
    public void testDefaults() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(2, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("ignoredField");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferOptions.class);
        Assert.assertNotNull(fieldAnnotation);
    }

    @FlatBufferTable
    @FlatBufferOptions
    private class TestTableDefault {

        @FlatBufferOptions
        public String ignoredField;

        public String notIgnoredField;
    }
}