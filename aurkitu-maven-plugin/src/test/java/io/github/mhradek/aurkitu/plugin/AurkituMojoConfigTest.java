package io.github.mhradek.aurkitu.plugin;

import io.github.mhradek.aurkitu.plugin.core.SchemaGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style tests that exercise SchemaGenerator.Config the same way
 * AurkituMojo does, covering the main configuration combinations.
 */
class AurkituMojoConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void configProducesSchemaFileWithIdentifierAndVersion() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                "AB12",
                List.of(),
                true,
                false,
                true,
                true,
                false);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("mojo-check", "test.mojo", out);

        Path schema = out.toPath().resolve("mojo-check.fbs");
        assertTrue(Files.exists(schema));
        String content = Files.readString(schema);
        assertTrue(content.contains("file_identifier \"AB12\";"));
        assertTrue(content.contains("// @version:"));
        assertTrue(content.contains("table SampleRootTable"));
        // snake_case default
        assertTrue(content.contains("nested_map:") || content.contains("base_id:"));
    }

    @Test
    void snakeCaseCanBeDisabled() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                null,
                List.of(),
                true,
                false,
                false, // snakeCaseFields = false
                false,
                false);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("camel", "test.camel", out);

        String content = Files.readString(out.toPath().resolve("camel.fbs"));
        assertTrue(content.contains("nestedMap:") || content.contains("baseId:"));
        assertFalse(content.contains("nested_map:"));
    }

    @Test
    void includesAreEmitted() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                false,
                true,
                null,
                List.of("shared.fbs"),
                true,
                false,
                true,
                false,
                false);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("inc", "test.inc", out);

        String content = Files.readString(out.toPath().resolve("inc.fbs"));
        assertTrue(content.contains("include \"shared.fbs\";"));
    }

    @Test
    void failOnValidationIssuesThrowsWhenProblemsExist() {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("java.lang"), // no annotated types
                true,
                true,
                null,
                List.of(),
                true,
                false,
                true,
                false,
                true); // failOnValidationIssues = true
        SchemaGenerator generator = new SchemaGenerator(config);

        assertThrows(RuntimeException.class, () ->
                generator.generate("fail", "test.fail", tempDir.toFile()));
    }

    @Test
    void cacheSkipsRegenerationWhenHashMatches() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                null,
                List.of(),
                true,
                true, // cacheEnabled
                true,
                false,
                false);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();

        generator.generate("cached", "test.cache", out);
        Path schema = out.toPath().resolve("cached.fbs");
        Path cache = out.toPath().resolve("cached.fbs.cache");
        assertTrue(Files.exists(schema));
        assertTrue(Files.exists(cache));

        long firstMtime = Files.getLastModifiedTime(schema).toMillis();
        Thread.sleep(10);
        generator.generate("cached", "test.cache", out);
        long secondMtime = Files.getLastModifiedTime(schema).toMillis();
        assertEquals(firstMtime, secondMtime, "Cache hit should not rewrite the schema");
    }
}
