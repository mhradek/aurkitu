package io.github.mhradek.aurkitu.annotations.types;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class EnumTypeTest {

    @Test
    public void testDefaults() {
        for (EnumType type : EnumType.values()) {
            switch (type) {
                case BYTE:
                    assertEquals(byte.class, type.targetClass);
                    assertEquals("byte", type.toString());
                    break;
                case UBYTE:
                    assertEquals(byte.class, type.targetClass);
                    assertEquals("ubyte", type.toString());
                    break;
                case SHORT:
                    assertEquals(short.class, type.targetClass);
                    assertEquals("short", type.toString());
                    break;
                case USHORT:
                    assertEquals(short.class, type.targetClass);
                    assertEquals("ushort", type.toString());
                    break;
                case INT:
                    assertEquals(int.class, type.targetClass);
                    assertEquals("int", type.toString());
                    break;
                case UINT:
                    assertEquals(int.class, type.targetClass);
                    assertEquals("uint", type.toString());
                    break;
                case LONG:
                    assertEquals(long.class, type.targetClass);
                    assertEquals("long", type.toString());
                    break;
                case ULONG:
                    assertEquals(long.class, type.targetClass);
                    assertEquals("ulong", type.toString());
                    break;
                default:
                    fail("Undefined enum");
                    break;
            }
        }
    }
}