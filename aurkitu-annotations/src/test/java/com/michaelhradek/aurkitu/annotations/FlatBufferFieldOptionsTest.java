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

    @Test
    public void testUnsignedForced() throws NoSuchFieldException {
        TestTableDefault testTable = new TestTableDefault();

        Field optionsDifferentTypeUintField = testTable.getClass().getField("optionsDifferentTypeUint");
        Annotation[] optionsDifferentTypeUintFieldDeclaredAnnotations = optionsDifferentTypeUintField.getDeclaredAnnotations();
        Assert.assertNotNull(optionsDifferentTypeUintFieldDeclaredAnnotations);
        Assert.assertEquals(1, optionsDifferentTypeUintFieldDeclaredAnnotations.length);

        Annotation fieldAnnotation = optionsDifferentTypeUintField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);
        Assert.assertEquals(FieldType.UINT, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());

        Field optionsDifferentTypeUlongField = testTable.getClass().getField("optionsDifferentTypeUlong");
        Annotation[] optionsDifferentTypeUlongFieldDeclaredAnnotations = optionsDifferentTypeUlongField.getDeclaredAnnotations();
        Assert.assertNotNull(optionsDifferentTypeUlongFieldDeclaredAnnotations);
        Assert.assertEquals(1, optionsDifferentTypeUlongFieldDeclaredAnnotations.length);

        fieldAnnotation = optionsDifferentTypeUlongField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);
        Assert.assertEquals(FieldType.ULONG, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());

        Field optionsDifferentTypeUbyteField = testTable.getClass().getField("optionsDifferentTypeUbyte");
        Annotation[] optionsDifferentTypeUbyteFieldDeclaredAnnotations = optionsDifferentTypeUbyteField.getDeclaredAnnotations();
        Assert.assertNotNull(optionsDifferentTypeUbyteFieldDeclaredAnnotations);
        Assert.assertEquals(1, optionsDifferentTypeUbyteFieldDeclaredAnnotations.length);

        fieldAnnotation = optionsDifferentTypeUbyteField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);
        Assert.assertEquals(FieldType.UBYTE, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());

        Field optionalDifferentTypeUshortField = testTable.getClass().getField("optionalDifferentTypeUshort");
        Annotation[] optionalDifferentTypeUshortFieldDeclaredAnnotations = optionalDifferentTypeUshortField.getDeclaredAnnotations();
        Assert.assertNotNull(optionalDifferentTypeUshortFieldDeclaredAnnotations);
        Assert.assertEquals(1, optionalDifferentTypeUshortFieldDeclaredAnnotations.length);

        fieldAnnotation = optionalDifferentTypeUshortField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);
        Assert.assertEquals(FieldType.USHORT, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());

        Field optionalDifferentTypeGeneralByteVectorField = testTable.getClass().getField("optionalDifferentTypeGeneralByteVector");
        Annotation[] optionalDifferentTypeGeneralByteVectorFieldDeclaredAnnotations = optionalDifferentTypeGeneralByteVectorField.getDeclaredAnnotations();
        Assert.assertNotNull(optionalDifferentTypeGeneralByteVectorFieldDeclaredAnnotations);
        Assert.assertEquals(1, optionalDifferentTypeGeneralByteVectorFieldDeclaredAnnotations.length);

        fieldAnnotation = optionalDifferentTypeGeneralByteVectorField.getAnnotation(FlatBufferFieldOptions.class);
        Assert.assertNotNull(fieldAnnotation);
        Assert.assertEquals(FieldType.UBYTE, ((FlatBufferFieldOptions) fieldAnnotation).fieldType());
    }

    @FlatBufferTable
    private class TestTableDefault {

        @FlatBufferFieldOptions(fieldType = FieldType.STRING)
        public String optionsField;

        @FlatBufferFieldOptions(fieldType = FieldType.ARRAY, useFullName = true, defaultValue = TEST_DEFAULT_VALUE)
        @Deprecated
        public String[] optionsFieldExtended;

        // While Java doesn't support these, other locations/native does so let's force them.
        @FlatBufferFieldOptions(fieldType = FieldType.UINT)
        public int optionsDifferentTypeUint; // Force UNINT
        @FlatBufferFieldOptions(fieldType = FieldType.ULONG)
        public long optionsDifferentTypeUlong; // Force ULONG
        @FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
        public byte optionsDifferentTypeUbyte; // Force UBYTE
        @FlatBufferFieldOptions(fieldType = FieldType.USHORT)
        public short optionalDifferentTypeUshort; // Force USHORT
        @FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
        public String optionalDifferentTypeGeneralByteVector; // Force UBYTE for String
    }
}