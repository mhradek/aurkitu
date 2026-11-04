package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamingTest {

    @Test
    void toSnakeCaseHandlesNullAndEmpty() {
        assertNull(Naming.toSnakeCase(null));
        assertEquals("", Naming.toSnakeCase(""));
    }

    @Test
    void toSnakeCaseConvertsCamelCase() {
        assertEquals("nested_map", Naming.toSnakeCase("nestedMap"));
        assertEquals("base_id", Naming.toSnakeCase("baseId"));
        assertEquals("already_snake", Naming.toSnakeCase("already_snake"));
        // Consecutive uppercase gets an underscore before each letter
        assertEquals("x_m_l_parser", Naming.toSnakeCase("XMLParser"));
        assertEquals("a", Naming.toSnakeCase("a"));
    }

    @Test
    void enumConstantNameExtractsLastSegment() {
        assertEquals("STRUCT", Naming.enumConstantName("io.github.mhradek.aurkitu.annotations.FlatBufferTable.TableStructureType.STRUCT"));
        assertEquals("UNION", Naming.enumConstantName("UNION"));
        assertEquals("", Naming.enumConstantName(null));
        assertEquals("BYTE", Naming.enumConstantName("FieldType.BYTE"));
    }
}
