package io.github.mhradek.aurkitu.annotations.types;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class FieldTypeTest {

    @Test
    public void testDefaults() {
        assertEquals(15, FieldType.values().length, "every value is checked below");
        for (FieldType type : FieldType.values()) {
            switch (type) {
                case BOOL:
                    assertEquals(boolean.class, type.targetClass);
                    assertEquals("bool", type.toString());
                    break;
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
                case FLOAT:
                    assertEquals(float.class, type.targetClass);
                    assertEquals("float", type.toString());
                    break;
                case DOUBLE:
                    assertEquals(double.class, type.targetClass);
                    assertEquals("double", type.toString());
                    break;
                case STRING:
                    assertEquals(String.class, type.targetClass);
                    assertEquals("string", type.toString());
                    break;
                case IDENT:
                    assertNull(type.targetClass);
                    assertEquals("ident", type.toString());
                    break;
                case ARRAY:
                    assertNull(type.targetClass);
                    assertEquals("array", type.toString());
                    break;
                case MAP:
                    assertNull(type.targetClass);
                    assertEquals("map", type.toString());
                    break;
                default:
                    fail("Undefined enum");
                    break;
            }
        }
    }
}