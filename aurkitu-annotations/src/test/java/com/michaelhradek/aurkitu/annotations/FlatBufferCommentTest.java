package com.michaelhradek.aurkitu.annotations;

import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FlatBufferCommentTest {

    private final static String TEST_DEFAULT_COMMENT = "testDefaultValueForField";
    private final static String TEST_EXTENDED_COMMENT = "testExtendedValueForField";

    @Test
    public void testDefaults() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(1, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("testFieldDefault");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferComment.class);
        Assert.assertNotNull(fieldAnnotation);

        Assert.assertEquals(TEST_DEFAULT_COMMENT, ((FlatBufferComment) fieldAnnotation).comment());
    }

    @Test
    public void testExtended() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();
        Annotation[] listClassAnnotations = testTable.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(1, listClassAnnotations.length);

        Field optionsField = testTable.getClass().getField("testFieldExtended");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(2, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferComment.class);
        Assert.assertNotNull(fieldAnnotation);

        Assert.assertEquals(TEST_EXTENDED_COMMENT, ((FlatBufferComment) fieldAnnotation).comment());
    }

    @FlatBufferTable
    private class TestTableDefault {

        @FlatBufferComment(comment = TEST_DEFAULT_COMMENT)
        public String testFieldDefault;

        @FlatBufferComment(comment = TEST_EXTENDED_COMMENT)
        @Deprecated
        public String[] testFieldExtended;
    }
}