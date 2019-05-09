package com.michaelhradek.aurkitu.annotations.types;

import org.junit.Assert;
import org.junit.Test;

public class FieldTypeTest {

    @Test
    public void testDefaults() {
        for (FieldType type : FieldType.values()) {
            switch (type) {
                case BOOL:
                    Assert.assertEquals(boolean.class, type.targetClass);
                    Assert.assertEquals("bool", type.toString());
                    break;
                case BYTE:
                    Assert.assertEquals(byte.class, type.targetClass);
                    Assert.assertEquals("byte", type.toString());
                    break;
                case UBYTE:
                    Assert.assertEquals(byte.class, type.targetClass);
                    Assert.assertEquals("ubyte", type.toString());
                    break;
                case SHORT:
                    Assert.assertEquals(short.class, type.targetClass);
                    Assert.assertEquals("short", type.toString());
                    break;
                case USHORT:
                    Assert.assertEquals(short.class, type.targetClass);
                    Assert.assertEquals("ushort", type.toString());
                    break;
                case INT:
                    Assert.assertEquals(int.class, type.targetClass);
                    Assert.assertEquals("int", type.toString());
                    break;
                case UINT:
                    Assert.assertEquals(int.class, type.targetClass);
                    Assert.assertEquals("uint", type.toString());
                    break;
                case LONG:
                    Assert.assertEquals(long.class, type.targetClass);
                    Assert.assertEquals("long", type.toString());
                    break;
                case ULONG:
                    Assert.assertEquals(long.class, type.targetClass);
                    Assert.assertEquals("ulong", type.toString());
                    break;
                case FLOAT:
                    Assert.assertEquals(float.class, type.targetClass);
                    Assert.assertEquals("float", type.toString());
                    break;
                case DOUBLE:
                    Assert.assertEquals(double.class, type.targetClass);
                    Assert.assertEquals("double", type.toString());
                    break;
                case STRING:
                    Assert.assertEquals(String.class, type.targetClass);
                    Assert.assertEquals("string", type.toString());
                    break;
                case IDENT:
                    Assert.assertNull(type.targetClass);
                    Assert.assertEquals("ident", type.toString());
                    break;
                case ARRAY:
                    Assert.assertNull(type.targetClass);
                    Assert.assertEquals("array", type.toString());
                    break;
                case MAP:
                    Assert.assertNull(type.targetClass);
                    Assert.assertEquals("map", type.toString());
                    break;
                default:
                    Assert.fail("Undefined enum");
                    break;
            }
        }
    }
}