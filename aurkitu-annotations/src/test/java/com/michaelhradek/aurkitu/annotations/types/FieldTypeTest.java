package com.michaelhradek.aurkitu.annotations.types;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FieldTypeTest {

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
    public void testDefaults() {
        for (FieldType type : FieldType.values()) {
            switch (type) {
                case BOOL:
                    Assert.assertEquals(boolean.class, type.targetClass);
                    break;
                case BYTE:
                    Assert.assertEquals(byte.class, type.targetClass);
                    break;
                case UBYTE:
                    Assert.assertEquals(byte.class, type.targetClass);
                    break;
                case SHORT:
                    Assert.assertEquals(short.class, type.targetClass);
                    break;
                case USHORT:
                    Assert.assertEquals(short.class, type.targetClass);
                    break;
                case INT:
                    Assert.assertEquals(int.class, type.targetClass);
                    break;
                case UINT:
                    Assert.assertEquals(int.class, type.targetClass);
                    break;
                case LONG:
                    Assert.assertEquals(long.class, type.targetClass);
                    break;
                case ULONG:
                    Assert.assertEquals(long.class, type.targetClass);
                    break;
                case FLOAT:
                    Assert.assertEquals(float.class, type.targetClass);
                    break;
                case DOUBLE:
                    Assert.assertEquals(double.class, type.targetClass);
                    break;
                case STRING:
                    Assert.assertEquals(String.class, type.targetClass);
                    break;
                case IDENT:
                    Assert.assertNull(type.targetClass);
                    break;
                case ARRAY:
                    Assert.assertNull(type.targetClass);
                    break;
                case MAP:
                    Assert.assertNull(type.targetClass);
                    break;
                default:
                    Assert.fail("Undefined enum");
            }
        }
    }
}