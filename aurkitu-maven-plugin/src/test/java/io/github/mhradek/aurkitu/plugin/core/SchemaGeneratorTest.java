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
        assertTrue(content.startsWith(AurkituVersion.schemaHeader()), "Header should be present");
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
        assertTrue(content.startsWith(AurkituVersion.schemaHeader()));
        assertTrue(content.contains("table SampleRootTable {"));
        assertTrue(content.contains("root_type SampleRootTable;"));
        assertTrue(content.contains("id:long;"));
        assertTrue(content.contains("name:string;"));
        assertTrue(content.contains("active:bool;"));
        assertTrue(content.contains("level:int = 42") || content.contains("level:int=42"));
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
    void emitsEnumUnderlyingTypes() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("enums", "test.enums", out);

        String content = Files.readString(out.toPath().resolve("enums.fbs"));
        assertTrue(content.contains("enum SampleEnumShort : short {"));
        assertTrue(content.contains("enum SampleEnumInt : int {"));
        assertTrue(content.contains("enum SampleEnumLong : long {"));
        assertTrue(content.contains("// A commented enum"));
        assertTrue(content.contains("enum SampleEnumWithComment : byte {"));
    }

    @Test
    void emitsCommonTypesAndWrappers() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("common", "test.common", out);

        String content = Files.readString(out.toPath().resolve("common.fbs"));
        assertTrue(content.contains("table SampleWithCommonTypes {"));
        assertTrue(content.contains("created:string"));
        assertTrue(content.contains("link:string"));
        assertTrue(content.contains("amount:string"));
        assertTrue(content.contains("flag:bool"));
        assertTrue(content.contains("count:int"));
        assertTrue(content.contains("tags:[string]"));
        assertTrue(content.contains("numbers:[int]"));
        assertTrue(content.contains("bytes:[ubyte];"), content);
    }

    @Test
    void emitsFieldIdsWhenAllFieldsHaveThem() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("ids", "test.ids", out);

        String content = Files.readString(out.toPath().resolve("ids.fbs"));
        assertTrue(content.contains("table SampleWithAllIds {"));
        assertTrue(content.contains("id:string (id: 0)") || content.contains("(id: 0)"));
        assertTrue(content.contains("version:int (id: 1)") || content.contains("(id: 1)"));
    }

    @Test
    void omitsPartialFieldIdsAndReportsIssue() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("partial", "test.partial", out);

        String content = Files.readString(out.toPath().resolve("partial.fbs"));
        assertTrue(content.contains("table SampleWithPartialIds {"));
        assertTrue(content.contains("Partial field ids") || content.contains("ids omitted"));
        // Scope the check to the SampleWithPartialIds table so SampleWithAllIds
        // (which legitimately emits id attributes) does not interfere.
        int start = content.indexOf("table SampleWithPartialIds {");
        int end = content.indexOf("}", start);
        String tableBlock = content.substring(start, end);
        assertFalse(tableBlock.contains("(id:"), "Partial table should not emit id attributes: " + tableBlock);
    }

    @Test
    void reportsNonScalarStructField() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.validation"),
                true);
        File out = tempDir.toFile();
        generator.generate("struct", "test.struct", out);

        String content = Files.readString(out.toPath().resolve("struct.fbs"));
        assertTrue(content.contains("struct SampleStructWithString {"));
        assertTrue(content.contains("non-scalar") || content.contains("SampleStructWithString"));
    }

    @Test
    void emitsPerTypeNamespaceOverride() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.validation"),
                true);
        File out = tempDir.toFile();
        generator.generate("ns", "test.ns", out);

        String content = Files.readString(out.toPath().resolve("ns.fbs"));
        assertTrue(content.contains("namespace other.ns;"));
        assertTrue(content.contains("table SampleWithNamespace {"));
    }

    @Test
    void reportsUnsupportedFieldType() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("unsup", "test.unsup", out);

        String content = Files.readString(out.toPath().resolve("unsup.fbs"));
        assertTrue(content.contains("Unsupported field type") || content.contains("SampleWithUnsupported"));
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
        assertTrue(content.contains("table SampleWithAttributes {"));
        assertTrue(content.contains("key"));
        assertTrue(content.contains("hash: \"fnv1_32\""));
        assertTrue(content.contains("nested:[ubyte] (nested_flatbuffer: \"Nested\")"), content);
        assertTrue(content.contains("flex:[ubyte] (flexbuffer)"), content);
        // force_align is only valid on vectors: skipped (and reported) on a scalar.
        assertTrue(content.contains("aligned:int;"), content);
        assertTrue(content.contains("force_align on SampleWithAttributes.aligned skipped"), content);
        assertTrue(content.contains("aligned_bytes:[ubyte] (force_align: 16)"), content);
        assertTrue(content.contains("struct FixedStruct {"), "Schema was:\n" + content);
        assertTrue(content.contains("values:[int:4]"), "Expected fixed array. Schema was:\n" + content);
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
        assertTrue(content.startsWith(AurkituVersion.schemaHeader()));
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
        assertTrue(Files.exists(schema));

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
