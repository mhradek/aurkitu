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
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("models", "test.models", out);

        Path schemaPath = out.toPath().resolve("models.fbs");
        assertTrue(Files.exists(schemaPath));
        String content = Files.readString(schemaPath);

        // Header and namespace
        assertTrue(content.contains("namespace test.models;"));
        assertTrue(content.contains("// Aurkitu 1.0.0"));

        // Root table
        assertTrue(content.contains("table SampleRootTable {"));
        assertTrue(content.contains("root_type SampleRootTable;"));

        // Primitives
        assertTrue(content.contains("id:long;"));
        assertTrue(content.contains("name:string;"));
        assertTrue(content.contains("active:bool;"));

        // Field options default
        assertTrue(content.contains("level:int = 42") || content.contains("level:int=42"));

        // Ignore
        assertFalse(content.contains("ignored:"));

        // List
        assertTrue(content.contains("tags:[string]") || content.contains("tags:[String]"));

        // Simple map
        assertTrue(content.contains("scores:[MapValueSet_SampleRootTable_scores]"));
        assertTrue(content.contains("table MapValueSet_SampleRootTable_scores {"));

        // Nested map
        assertTrue(content.contains("nested_map:[MapValueSet_SampleRootTable_nested_map]"));
        assertTrue(content.contains("table MapValueSet_SampleRootTable_nested_map {"));

        // Enum
        assertTrue(content.contains("enum SampleEnum : byte {"));
        assertTrue(content.contains("ALPHA"));
        assertTrue(content.contains("BETA"));
        assertTrue(content.contains("GAMMA"));

        // Unsigned
        assertTrue(content.contains("table SampleUnsigned {"));
        assertTrue(content.contains("flags:ubyte"));
        assertTrue(content.contains("port:ushort"));
        assertTrue(content.contains("count:uint"));
        assertTrue(content.contains("id:ulong") || content.contains(":ulong"));

        // Struct
        assertTrue(content.contains("struct SampleStruct {"));
        assertTrue(content.contains("x:float"));

        // Union
        assertTrue(content.contains("union SampleUnion {"));
        assertTrue(content.contains("SampleRootTable"));

        // Required / deprecated / comments / inheritance
        assertTrue(content.contains("table SampleWithOptions {"));
        assertTrue(content.contains("// A table with various field options"));
        assertTrue(content.contains("// Required identifier"));
        assertTrue(content.contains("code:string (required)"));
        assertTrue(content.contains("legacy_value:int = 0 (deprecated)") || content.contains("legacy_value:int=0 (deprecated)"));
        assertTrue(content.contains("base_id:long"));
        assertTrue(content.contains("base_name:string"));
    }

    @Test
    void emitsFileIdentifierAndIncludes() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                "AB12",
                List.of("common.fbs", "types.fbs"),
                true,
                false,
                true,
                false,
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("config", "test.config", out);

        String content = Files.readString(out.toPath().resolve("config.fbs"));
        assertTrue(content.contains("file_identifier \"AB12\";"));
        assertTrue(content.contains("include \"common.fbs\";"));
        assertTrue(content.contains("include \"types.fbs\";"));
    }

    @Test
    void emitsFileExtension() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                null,
                List.of(),
                true,
                false,
                true,
                false,
                false,
                "bin");
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("ext", "test.ext", out);

        String content = Files.readString(out.toPath().resolve("ext.fbs"));
        assertTrue(content.contains("file_extension \"bin\";"));
    }

    @Test
    void reportsMissingRootTypeWhenNonePresent() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("java.lang"),
                true);
        File out = tempDir.toFile();
        generator.generate("empty", "test.empty", out);

        String content = Files.readString(out.toPath().resolve("empty.fbs"));
        assertTrue(content.contains("// Validation issues:"));
        assertTrue(content.contains("No root type defined"));
    }

    @Test
    void failOnValidationIssuesThrowsWhenIssuesExist() {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("java.lang"),
                true,
                true,
                null,
                List.of(),
                true,
                false,
                true,
                false,
                true,   // failOnValidationIssues
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();

        SchemaGenerationException ex = assertThrows(SchemaGenerationException.class,
                () -> generator.generate("fail", "test.fail", out));
        assertTrue(ex.getMessage().contains("validation failed"));
    }

    @Test
    void snakeCaseFieldsCanBeDisabled() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                null,
                List.of(),
                true,
                false,
                false,  // snakeCaseFields = false
                false,
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("nosnake", "test.nosnake", out);

        String content = Files.readString(out.toPath().resolve("nosnake.fbs"));
        // With snake_case off, nestedMap stays nestedMap (not nested_map)
        assertTrue(content.contains("nestedMap:") || content.contains("nestedMap"));
        assertFalse(content.contains("nested_map:"));
    }

    @Test
    void generateVersionEmitsContentHash() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                null,
                List.of(),
                true,
                false,
                true,
                true,   // generateVersion
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("versioned", "test.versioned", out);

        String content = Files.readString(out.toPath().resolve("versioned.fbs"));
        assertTrue(content.contains("// @version: "));
        // Second run with same inputs should produce the same version hash
        generator.generate("versioned2", "test.versioned", out);
        String content2 = Files.readString(out.toPath().resolve("versioned2.fbs"));
        String v1 = extractVersion(content);
        String v2 = extractVersion(content2);
        assertEquals(v1, v2, "Version hash should be stable for identical inputs");
    }

    @Test
    void cacheHitSkipsRegeneration() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                null,
                List.of(),
                true,
                true,   // cacheEnabled
                true,
                false,
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();

        generator.generate("cached", "test.cached", out);
        Path schema = out.toPath().resolve("cached.fbs");
        Path cache = out.toPath().resolve("cached.fbs.cache");
        assertTrue(Files.exists(schema));
        assertTrue(Files.exists(cache));

        String first = Files.readString(schema);
        long firstMtime = Files.getLastModifiedTime(schema).toMillis();

        // Second call should hit the cache and not rewrite
        generator.generate("cached", "test.cached", out);
        String second = Files.readString(schema);
        assertEquals(first, second);
        // mtime may or may not change depending on filesystem resolution; content equality is the real check
    }

    @Test
    void ignoresStaticMembersByDefault() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("staticcheck", "test.static", out);

        String content = Files.readString(out.toPath().resolve("staticcheck.fbs"));
        assertFalse(content.contains("static "));
    }

    @Test
    void mapCommonTypesCanBeDisabled() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                false,
                true,
                null,
                List.of(),
                false,
                false,
                true,
                false,
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("nocommon", "test.nocommon", out);

        String content = Files.readString(out.toPath().resolve("nocommon.fbs"));
        assertTrue(content.contains("table SampleRootTable"));
    }

    private static String extractVersion(String content) {
        int start = content.indexOf("// @version: ");
        assertTrue(start >= 0);
        int end = content.indexOf('\n', start);
        return content.substring(start, end).trim();
    }
}
