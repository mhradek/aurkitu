package io.github.mhradek.aurkitu.plugin;

import io.github.mhradek.aurkitu.plugin.core.SchemaGenerationException;
import io.github.mhradek.aurkitu.plugin.core.SchemaGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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
                false, null);
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
                false,
                null);
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
                false,
                null);
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
                true,
                null); // failOnValidationIssues = true
        SchemaGenerator generator = new SchemaGenerator(config);

        assertThrows(SchemaGenerationException.class, () ->
                generator.generate("fail", "test.fail", tempDir.toFile()));
    }

    @Test
    void cacheKeepsAnUnchangedSchemaAndRewritesAChangedOne() throws Exception {
        File out = tempDir.toFile();
        Path schema = out.toPath().resolve("cached.fbs");
        SchemaGenerator generator = new SchemaGenerator(cachingConfig(false));

        generator.generate("cached", "test.cache", out);
        String generated = Files.readString(schema);
        FileTime old = FileTime.fromMillis(0);
        Files.setLastModifiedTime(schema, old);

        generator.generate("cached", "test.cache", out);
        assertEquals(old, Files.getLastModifiedTime(schema), "unchanged schema must not be rewritten");

        // Any change to the output (here: a stale or edited file) is detected, unlike a type-name hash.
        Files.writeString(schema, generated.replace("table SampleRootTable", "table Stale"));
        Files.setLastModifiedTime(schema, old);
        generator.generate("cached", "test.cache", out);
        assertEquals(generated, Files.readString(schema));
        assertNotEquals(old, Files.getLastModifiedTime(schema));
        assertFalse(Files.exists(out.toPath().resolve("cached.fbs.cache")), "no separate cache file");
    }

    @Test
    void cacheStillValidates() throws Exception {
        File out = tempDir.toFile();
        new SchemaGenerator(cachingConfig(false)).generate("cached", "test.cache", out);
        // Same, unchanged output: failOnValidationIssues must still apply.
        SchemaGenerator strict = new SchemaGenerator(cachingConfig(true));
        assertThrows(SchemaGenerationException.class, () -> strict.generate("cached", "test.cache", out));
    }

    private static SchemaGenerator.Config cachingConfig(boolean failOnValidationIssues) {
        return new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true, true, null, List.of(), true,
                true, // cacheEnabled
                true, false, failOnValidationIssues, null);
    }
}
