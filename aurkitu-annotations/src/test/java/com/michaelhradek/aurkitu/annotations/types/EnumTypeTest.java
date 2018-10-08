package com.michaelhradek.aurkitu.annotations.types;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EnumTypeTest {

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
        for (EnumType type : EnumType.values()) {
            switch (type) {
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
                default:
                    Assert.fail("Undefined enum");
            }
        }
    }
}