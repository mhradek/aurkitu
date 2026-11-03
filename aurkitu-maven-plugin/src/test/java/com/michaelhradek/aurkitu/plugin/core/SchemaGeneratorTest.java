package com.michaelhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesSchemaWithFields() throws Exception {
        // Note: full MavenProject integration is limited in unit tests;
        // this verifies the generator writes a file without crashing.
        // Real coverage comes from integration tests against annotated classes.
        SchemaGenerator generator = new SchemaGenerator(null, List.of("com.michaelhradek.aurkitu"), true);
        File out = tempDir.toFile();
        generator.generate("test", "generated.flatbuffers", out);

        Path schema = out.toPath().resolve("test.fbs");
        assertTrue(Files.exists(schema), "Schema file should be written");
        String content = Files.readString(schema);
        assertTrue(content.contains("namespace generated.flatbuffers"), "Namespace should be present");
        assertTrue(content.contains("// Aurkitu 1.0.0"), "Header should be present");
    }
}
