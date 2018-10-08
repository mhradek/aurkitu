package com.michaelhradek.aurkitu.annotations;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FlatBufferEnumTypeFieldTest {

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testDefault() throws NoSuchFieldException {
        TestEnumDefault testEnum = TestEnumDefault.VALUE;
        Annotation[] listClassAnnotations = testEnum.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(1, listClassAnnotations.length);

        Field optionsField = testEnum.getClass().getField("enumValue");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(1, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferEnumTypeField.class);
        Assert.assertNotNull(fieldAnnotation);
    }

    @Test
    public void testExtended() throws NoSuchFieldException {
        TestEnumDefault testEnum = TestEnumDefault.VALUE;
        Annotation[] listClassAnnotations = testEnum.getClass().getAnnotations();
        Assert.assertNotNull(listClassAnnotations);
        Assert.assertEquals(1, listClassAnnotations.length);

        Field optionsField = testEnum.getClass().getField("enumName");
        Annotation[] listFieldAnnotations = optionsField.getDeclaredAnnotations();
        Assert.assertNotNull(listFieldAnnotations);
        Assert.assertEquals(2, listFieldAnnotations.length);

        Annotation fieldAnnotation = optionsField.getAnnotation(FlatBufferEnumTypeField.class);
        Assert.assertNotNull(fieldAnnotation);
    }


    @FlatBufferEnum
    private enum TestEnumDefault {

        VALUE(1, "test");

        TestEnumDefault(int value, String name) {
            this.enumValue = value;
            this.enumName = name;
        }

        @FlatBufferEnumTypeField
        public int enumValue;

        @FlatBufferEnumTypeField
        @Deprecated
        public String enumName;
    }
}