package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acronym-aware snake_case is the default since 1.2.0 ({@code userID} as {@code user_id}); turning it off
 * gives the names 1.0 and 1.1 wrote ({@code user_i_d}).
 */
class AcronymDefaultTest {

    private static final String PACKAGE = "io.github.mhradek.aurkitu.plugin.core.acronyms";

    @TempDir
    Path tempDir;

    @Test
    void acronymsStayOneWordByDefault() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(List.of(PACKAGE), true);
        assertTrue(config.snakeCaseAcronyms);
        assertEquals(schema("user_id", "profile_url"), generate(config));
    }

    @Test
    void turnedOffEveryCapitalStartsAWordAsIn11() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(List.of(PACKAGE), true).withSnakeCaseAcronyms(false);
        assertEquals(schema("user_i_d", "profile_u_r_l"), generate(config));
    }

    private String generate(SchemaGenerator.Config config) throws Exception {
        new SchemaGenerator(config).generate("acr", "app", tempDir.toFile());
        return Files.readString(tempDir.resolve("acr.fbs"));
    }

    private static String schema(String userId, String profileUrl) {
        return AurkituVersion.schemaHeader() + "\nnamespace app;\n\ntable Profile {\n  " + userId + ":long;\n  "
                + profileUrl + ":string;\n  xml_parser:string;\n}\n\nroot_type Profile;\n";
    }
}
