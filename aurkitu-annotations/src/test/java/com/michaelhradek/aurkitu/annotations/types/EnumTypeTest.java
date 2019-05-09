package com.michaelhradek.aurkitu.annotations.types;

import org.junit.Assert;
import org.junit.Test;

public class EnumTypeTest {

    @Test
    public void testDefaults() {
        for (EnumType type : EnumType.values()) {
            switch (type) {
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
                default:
                    Assert.fail("Undefined enum");
                    break;
            }
        }
    }
}