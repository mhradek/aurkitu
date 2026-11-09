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
    void acronymAwareSnakeCaseKeepsRunsOfCapitalsTogether() {
        String[][] cases = {
                {"userID", "user_id", "user_i_d"},
                {"XMLParser", "xml_parser", "x_m_l_parser"},
                {"parseHTTPResponse", "parse_http_response", "parse_h_t_t_p_response"},
                {"accountURL", "account_url", "account_u_r_l"},
                {"ID", "id", "i_d"},
                {"userId", "user_id", "user_id"},
                {"baseId", "base_id", "base_id"},
                {"address2Line", "address2_line", "address2_line"},
                {"userID2", "user_id2", "user_i_d2"},
                {"already_snake", "already_snake", "already_snake"},
                {"HTTP_Status", "http_status", "h_t_t_p_status"},
                {"a", "a", "a"},
                {"aB", "a_b", "a_b"},
                {"URLs", "urls", "u_r_ls"},
                {"userIDs", "user_ids", "user_i_ds"},
                {"URLsByHost", "urls_by_host", "u_r_ls_by_host"},
                {"IDsort", "i_dsort", "i_dsort"},
                {"HTTPServer", "http_server", "h_t_t_p_server"},
                {"HTTPSession", "http_session", "h_t_t_p_session"},
        };
        for (String[] c : cases) {
            assertEquals(c[1], Naming.toSnakeCase(c[0], true), c[0] + " with acronyms");
            assertEquals(c[2], Naming.toSnakeCase(c[0], false), c[0] + " without");
            assertEquals(c[2], Naming.toSnakeCase(c[0]), c[0] + " default");
        }
        assertNull(Naming.toSnakeCase(null, true));
        assertEquals("", Naming.toSnakeCase("", true));
        assertEquals("userID", Naming.fieldName("userID", false, true), "acronyms only apply with snake_case");
        assertEquals("user_id", Naming.fieldName("userID", true, true));
        assertEquals("outer_inner_id", Naming.fieldName("outer$InnerID", true, true));
    }

    @Test
    void enumConstantNameExtractsLastSegment() {
        assertEquals("STRUCT", Naming.enumConstantName("io.github.mhradek.aurkitu.annotations.FlatBufferTable.TableStructureType.STRUCT"));
        assertEquals("UNION", Naming.enumConstantName("UNION"));
        assertEquals("", Naming.enumConstantName(null));
        assertEquals("BYTE", Naming.enumConstantName("FieldType.BYTE"));
    }
}
