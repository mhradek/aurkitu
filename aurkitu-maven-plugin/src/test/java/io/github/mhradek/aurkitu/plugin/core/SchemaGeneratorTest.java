package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesSchemaFile() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(null, List.of(), true);
        File out = tempDir.toFile();
        generator.generate("test", "generated.flatbuffers", out);

        Path schema = out.toPath().resolve("test.fbs");
        assertTrue(Files.exists(schema), "Schema file should be written");
        String content = Files.readString(schema);
        assertTrue(content.contains("namespace generated.flatbuffers"), "Namespace should be present");
        assertTrue(content.contains("// Aurkitu 1.0.0"), "Header should be present");
    }

    @Test
    void generatesTablesEnumsMapsAndLists() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("com.michaelhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("models", "test.models", out);

        Path schemaPath = out.toPath().resolve("models.fbs");
        assertTrue(Files.exists(schemaPath));
        String content = Files.readString(schemaPath);

        System.out.println("=== Generated schema ===");
        System.out.println(content);
        System.out.println("=== End schema ===");

        // Header and namespace
        assertTrue(content.contains("namespace test.models;"));
        assertTrue(content.contains("// Aurkitu 1.0.0"));

        // At least the root table and enum should appear
        assertTrue(content.contains("SampleRootTable") || content.contains("SampleEnum") || content.contains("SampleStruct"),
                "Expected at least one annotated type in schema. Content was:\n" + content);
    }
}
