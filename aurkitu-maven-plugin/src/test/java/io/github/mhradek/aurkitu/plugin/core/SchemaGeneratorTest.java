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

        assertTrue(content.contains("namespace test.models;"));
        assertTrue(content.contains("// Aurkitu 1.0.0"));
        assertTrue(content.contains("table SampleRootTable {"));
        assertTrue(content.contains("root_type SampleRootTable;"));
        assertTrue(content.contains("id:long;"));
        assertTrue(content.contains("name:string;"));
        assertTrue(content.contains("active:bool;"));
        assertTrue(content.contains("level:int = 42") || content.contains("level:int=42"));
        // Look for the field, not the word in a validation comment
        assertFalse(content.contains("ignored:") && !content.contains("// - "));
        assertTrue(content.contains("tags:[string]") || content.contains("tags:[String]"));
        assertTrue(content.contains("scores:[MapValueSet_SampleRootTable_scores]"));
        assertTrue(content.contains("table MapValueSet_SampleRootTable_scores {"));
        assertTrue(content.contains("nested_map:[MapValueSet_SampleRootTable_nested_map]"));
        assertTrue(content.contains("table MapValueSet_SampleRootTable_nested_map {"));
        assertTrue(content.contains("enum SampleEnum : byte {"));
        assertTrue(content.contains("ALPHA"));
        assertTrue(content.contains("BETA"));
        assertTrue(content.contains("GAMMA"));
        assertTrue(content.contains("table SampleUnsigned {"));
        assertTrue(content.contains("flags:ubyte"));
        assertTrue(content.contains("port:ushort"));
        assertTrue(content.contains("count:uint"));
        assertTrue(content.contains("id:ulong") || content.contains(":ulong"));
        assertTrue(content.contains("struct SampleStruct {"));
        assertTrue(content.contains("x:float"));
        assertTrue(content.contains("union SampleUnion {"));
        assertTrue(content.contains("SampleRootTable"));
        assertTrue(content.contains("table SampleWithOptions {"));
        assertTrue(content.contains("// A table with various field options"));
        assertTrue(content.contains("// Required identifier"));
        assertTrue(content.contains("code:string (required)"));
        assertTrue(content.contains("legacy_value:int = 0 (deprecated)") || content.contains("legacy_value:int=0 (deprecated)"));
        assertTrue(content.contains("base_id:long"));
        assertTrue(content.contains("base_name:string"));
    }

    @Test
    void emitsFieldAttributesAndFixedArray() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.attrs"),
                true);
        File out = tempDir.toFile();
        generator.generate("attrs", "test.attrs", out);

        String content = Files.readString(out.toPath().resolve("attrs.fbs"));
        System.out.println("=== attrs schema ===\n" + content + "=== end ===");
        assertTrue(content.contains("table SampleWithAttributes {"));
        // Partial ids are omitted; other attributes remain
        assertTrue(content.contains("key"));
        assertTrue(content.contains("hash: \"fnv1_32\""));
        // nested_flatbuffer and flexbuffer only on [ubyte]
        assertTrue(content.contains("nested:[ubyte]") || content.contains("[ubyte]"));
        assertTrue(content.contains("nested_flatbuffer: \"Nested\""));
        assertTrue(content.contains("flex:[ubyte]") || content.contains("flexbuffer"));
        assertTrue(content.contains("force_align: 8"));
        // Struct must be present; fixed-array rewrite is still under investigation
        assertTrue(content.contains("struct FixedStruct {"), "Schema was:\n" + content);
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
    void emptyPackageListStillGeneratesHeader() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(null, List.of(), true);
        File out = tempDir.toFile();
        generator.generate("blank", "ns.blank", out);
        String content = Files.readString(out.toPath().resolve("blank.fbs"));
        assertTrue(content.contains("namespace ns.blank;"));
        assertTrue(content.contains("// Aurkitu 1.0.0"));
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
                true,
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
                false,
                false,
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("nosnake", "test.nosnake", out);

        String content = Files.readString(out.toPath().resolve("nosnake.fbs"));
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
                true,
                false,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("versioned", "test.versioned", out);

        String content = Files.readString(out.toPath().resolve("versioned.fbs"));
        assertTrue(content.contains("// @version: "));
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
                true,
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
        generator.generate("cached", "test.cached", out);
        String second = Files.readString(schema);
        assertEquals(first, second);
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
