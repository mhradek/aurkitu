package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Generator output for the {@code testmodels}, {@code validation} and {@code attrs} packages.
 * Declarations are compared whole (see {@link #block}), so a stray, missing or reordered
 * field fails the test instead of slipping past a substring check.
 */
class SchemaGeneratorTest {

    private static final String MODELS = "io.github.mhradek.aurkitu.plugin.core.testmodels";

    @TempDir
    Path tempDir;

    @Test
    void generatesHeaderAndNamespaceWhenNothingIsAnnotated() throws Exception {
        String content = generate(new SchemaGenerator(null, List.of("java.lang"), true), "test");
        assertEquals(AurkituVersion.schemaHeader() + "\nnamespace test.test;\n\n\n"
                + "// Validation issues:\n// - No root type defined\n", content);
    }

    @Test
    void anEmptyPackageListScansTheWholeClasspath() throws Exception {
        String content = generate(new SchemaGenerator(null, List.of(), true), "all");
        assertTrue(content.contains("table SampleRootTable {"), content); // testmodels
        assertTrue(content.contains("table WarnChild {"), content);       // warnings
        assertTrue(content.contains("enum BadEnum : byte {"), content);   // fixes
    }

    @Test
    void generatesTablesStructsEnumsAndUnions() throws Exception {
        String content = generate(defaults(), "models");

        assertTrue(content.startsWith(AurkituVersion.schemaHeader() + "\nnamespace test.models;\n\n"), content);
        assertEquals("""
                table SampleRootTable {
                  id:long;
                  name:string;
                  active:bool;
                  level:int = 42;
                  tags:[string];
                  scores:[MapValueSet_SampleRootTable_scores];
                  nested_map:[MapValueSet_SampleRootTable_nested_map];
                  status:SampleEnum;
                }
                """, block(content, "table SampleRootTable {"));
        assertFalse(content.contains("ignored"), "@FlatBufferIgnore field is left out");
        assertEquals("enum SampleEnum : byte {\n  ALPHA,\n  BETA,\n  GAMMA,\n}\n", block(content, "enum SampleEnum : byte {"));
        assertEquals("struct SampleStruct {\n  x:float;\n  y:float;\n  z:float;\n}\n", block(content, "struct SampleStruct {"));
        assertEquals("union SampleUnion {\n  SampleRootTable,\n  SampleStruct,\n}\n", block(content, "union SampleUnion {"));
        assertEquals("""
                table SampleUnsigned {
                  flags:ubyte;
                  port:ushort;
                  count:uint;
                  id:ulong;
                }
                """, block(content, "table SampleUnsigned {"));
        assertTrue(content.endsWith("root_type SampleRootTable;\n\n// Validation issues:\n"
                + "// - Partial field ids on SampleWithPartialIds; ids omitted (all fields or none must have an id)\n"
                + "// - Unsupported field type: SampleWithUnsupported.maybe\n\n// Warnings:\n"
                + "// - Table SampleWithOptions has inherited fields (from BaseEntity) after its own; adding a field to"
                + " SampleWithOptions moves them and breaks existing data. Give every field a fieldId to fix their positions.\n"),
                content);
    }

    @Test
    void mapsBecomeVectorsOfGeneratedTablesAndMapsOfMapsGetTheirOwn() throws Exception {
        String content = generate(defaults(), "maps");
        assertEquals("table MapValueSet_SampleRootTable_scores {\n  key:string;\n  value:int;\n}\n",
                block(content, "table MapValueSet_SampleRootTable_scores {"));
        assertEquals("table MapValueSet_SampleRootTable_nested_map {\n  key:string;\n"
                        + "  value:[MapValueSet_SampleRootTable_nested_map_value];\n}\n",
                block(content, "table MapValueSet_SampleRootTable_nested_map {"));
        assertEquals("table MapValueSet_SampleRootTable_nested_map_value {\n  key:string;\n  value:bool;\n}\n",
                block(content, "table MapValueSet_SampleRootTable_nested_map_value {"));
        assertTrue(content.indexOf("table MapValueSet_SampleRootTable_nested_map {")
                < content.indexOf("table MapValueSet_SampleRootTable_nested_map_value {"), "outer map table first");
    }

    @Test
    void fieldOptionsInheritanceAndCommentsAreWritten() throws Exception {
        String content = generate(defaults(), "options");
        assertEquals("""
                /// A table with various field options
                table SampleWithOptions {
                  /// Required identifier
                  code:string (required);
                  legacy_value:int = 0 (deprecated);
                  base_id:long;
                  base_name:string;
                }
                """, block(content, "/// A table with various field options"));
    }

    @Test
    void emitsEnumUnderlyingTypes() throws Exception {
        String content = generate(defaults(), "enums");
        assertEquals("enum SampleEnumShort : short {\n  ALPHA,\n  BETA,\n  GAMMA,\n}\n", block(content, "enum SampleEnumShort"));
        assertEquals("enum SampleEnumInt : int {\n  ONE,\n  TWO,\n  THREE,\n}\n", block(content, "enum SampleEnumInt"));
        assertEquals("enum SampleEnumLong : long {\n  A,\n  B,\n  C,\n}\n", block(content, "enum SampleEnumLong"));
        assertEquals("/// A commented enum\nenum SampleEnumWithComment : byte {\n  YES,\n  NO,\n}\n",
                block(content, "/// A commented enum"));
    }

    @Test
    void emitsCommonTypesWrappersCollectionsAndBinary() throws Exception {
        String content = generate(defaults(), "common");
        assertEquals("""
                table SampleWithCommonTypes {
                  created:string;
                  link:string;
                  amount:string;
                  flag:bool;
                  count:int;
                  tags:[string];
                  numbers:[int];
                  bytes:[ubyte];
                }
                """, block(content, "table SampleWithCommonTypes {"));
    }

    @Test
    void mapCommonTypesCanBeDisabled() throws Exception {
        String content = generate(config(false, true, true, false, false, null), "nocommon");
        assertEquals("""
                table SampleWithCommonTypes {
                  flag:bool;
                  count:int;
                  tags:[string];
                  numbers:[int];
                  bytes:[ubyte];
                }
                """, block(content, "table SampleWithCommonTypes {"));
        assertTrue(content.contains("// - Unsupported field type: SampleWithCommonTypes.created\n"
                + "// - Unsupported field type: SampleWithCommonTypes.link\n"
                + "// - Unsupported field type: SampleWithCommonTypes.amount\n"), content);
    }

    @Test
    void emitsFieldIdsWhenAllFieldsHaveThem() throws Exception {
        String content = generate(defaults(), "ids");
        assertEquals("table SampleWithAllIds {\n  id:string (id: 0);\n  version:int (id: 1);\n}\n",
                block(content, "table SampleWithAllIds {"));
    }

    @Test
    void omitsPartialFieldIdsAndReportsIssue() throws Exception {
        String content = generate(defaults(), "partial");
        assertEquals("table SampleWithPartialIds {\n  id:string;\n  name:string;\n}\n",
                block(content, "table SampleWithPartialIds {"));
        assertTrue(content.contains(
                "// - Partial field ids on SampleWithPartialIds; ids omitted (all fields or none must have an id)\n"), content);
    }

    @Test
    void unsupportedFieldTypeIsLeftOutAndReported() throws Exception {
        String content = generate(defaults(), "unsup");
        assertEquals("table SampleWithUnsupported {\n}\n", block(content, "table SampleWithUnsupported {"));
        assertTrue(content.contains("// - Unsupported field type: SampleWithUnsupported.maybe\n"), content);
    }

    @Test
    void reportsNonScalarStructFieldAndWritesPerTypeNamespaces() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(null, List.of("io.github.mhradek.aurkitu.plugin.core.validation"), true);
        String content = generate(generator, "struct");
        assertEquals(AurkituVersion.schemaHeader() + "\n" + """
                namespace test.struct;

                struct SampleStructWithString {
                  value:int;
                }

                namespace other.ns;

                table SampleWithNamespace {
                  value:int;
                }

                namespace test.struct;


                // Validation issues:
                // - Struct SampleStructWithString field name is non-scalar; structs may only contain scalars or other structs
                // - No root type defined
                """, content);
    }

    @Test
    void emitsFieldAttributesAndFixedArray() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(null, List.of("io.github.mhradek.aurkitu.plugin.core.attrs"), true);
        String content = generate(generator, "attrs");
        assertEquals("""
                table SampleWithAttributes {
                  id:string (key);
                  hash_field:int (hash: "fnv1_32");
                  nested:[ubyte] (nested_flatbuffer: "Nested");
                  flex:[ubyte] (flexbuffer);
                  aligned:int;
                  aligned_bytes:[ubyte] (force_align: 16);
                  full_name_enum:io.github.mhradek.aurkitu.plugin.core.testmodels.SampleEnum;
                  fixed_struct:FixedStruct;
                }
                """, block(content, "table SampleWithAttributes {"));
        assertEquals("struct FixedStruct {\n  values:[int:4];\n}\n", block(content, "struct FixedStruct {"));
        // force_align is only valid on vectors: skipped (and reported) on a scalar.
        assertTrue(content.contains("// - force_align on SampleWithAttributes.aligned skipped: only valid on vectors\n"), content);
    }

    @Test
    void emitsFileIdentifierIncludesAndExtensionInOrder() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(null, List.of(MODELS), true, true, "AB12",
                List.of("common.fbs", "types.fbs"), true, false, true, false, false, "bin");
        String content = generate(new SchemaGenerator(config), "config");
        assertTrue(content.startsWith(AurkituVersion.schemaHeader() + "\ninclude \"common.fbs\";\ninclude \"types.fbs\";\n\n"
                + "namespace test.config;\n\nfile_identifier \"AB12\";\n\nfile_extension \"bin\";\n\nenum SampleEnum"), content);
    }

    @Test
    void noFileIdentifierExtensionOrIncludesByDefault() throws Exception {
        String content = generate(defaults(), "plain");
        assertFalse(content.contains("include "), content);
        assertFalse(content.contains("file_identifier"), content);
        assertFalse(content.contains("file_extension"), content);
        assertFalse(content.contains("// @version:"), content);
    }

    @Test
    void failOnValidationIssuesThrowsAfterWritingTheSchemaWithItsIssues() throws Exception {
        SchemaGenerator generator = (config(true, true, true, false, true, null));
        SchemaGenerationException ex = assertThrows(SchemaGenerationException.class,
                () -> generator.generate("fail", "test.fail", tempDir.toFile()));
        assertEquals("Schema validation failed with 2 issue(s). See schema comments.", ex.getMessage());
        assertTrue(Files.readString(tempDir.resolve("fail.fbs")).contains("// Validation issues:\n"));
    }

    @Test
    void warningsAloneDoNotFailTheBuild() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(null,
                List.of("io.github.mhradek.aurkitu.plugin.core.warnings"), true, true, null, List.of(),
                true, false, true, false, true, null);
        String content = generate(new SchemaGenerator(config), "warn");
        assertFalse(content.contains("// Validation issues:"), content);
        assertTrue(content.endsWith("root_type WarnChild;\n\n// Warnings:\n// - Table WarnChild has inherited fields"
                + " (from WarnBase) after its own; adding a field to WarnChild moves them and breaks existing data."
                + " Give every field a fieldId to fix their positions.\n"), content);
    }

    @Test
    void validationOffWritesNoCommentsAndNeverFails() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(null, List.of(MODELS), false, true, null, List.of(),
                true, false, true, false, true, null);
        String content = generate(new SchemaGenerator(config), "novalidate");
        assertTrue(content.endsWith("root_type SampleRootTable;\n"), content);
    }

    @Test
    void snakeCaseFieldsCanBeDisabled() throws Exception {
        String content = generate(config(true, true, false, false, false, null), "nosnake");
        assertTrue(content.contains("  nestedMap:[MapValueSet_SampleRootTable_nestedMap];\n"), content);
        assertTrue(content.contains("table MapValueSet_SampleRootTable_nestedMapValue {"), content);
        assertTrue(content.contains("  legacyValue:int = 0 (deprecated);\n  baseId:long;\n  baseName:string;\n"), content);
        assertFalse(content.contains("_map"), content);
        assertFalse(content.contains("base_id"), content);
    }

    @Test
    void generateVersionEmitsAStableContentHash() throws Exception {
        SchemaGenerator generator = (config(true, true, true, true, false, null));
        String first = generate(generator, "versioned", "test.versioned");
        String again = generate(generator, "versioned2", "test.versioned");
        String other = generate(generator, "versioned3", "test.other");

        assertTrue(versionLine(first).matches("// @version: [0-9a-f]{1,8}"), versionLine(first));
        assertEquals(versionLine(first), versionLine(again), "stable for identical inputs");
        assertNotEquals(versionLine(first), versionLine(other), "changes with the content");
    }

    @Test
    void staticMembersAreIgnoredUnlessConfigured() throws Exception {
        assertFalse(generate(defaults(), "static").contains("instances"));

        SchemaGenerator.Config keep = new SchemaGenerator.Config(null, List.of(MODELS), true, false, null, List.of(),
                true, false, true, false, false, null);
        assertTrue(generate(new SchemaGenerator(keep), "nostatic").contains("  bytes:[ubyte];\n  instances:int;\n}"));
    }

    /** The declaration starting at {@code start}, through its closing brace line. */
    static String block(String content, String start) {
        int from = content.indexOf(start);
        assertTrue(from >= 0, "missing '" + start + "' in:\n" + content);
        int end = content.indexOf("\n}\n", from);
        assertTrue(end >= 0, "unterminated '" + start + "' in:\n" + content);
        return content.substring(from, end + 3);
    }

    private SchemaGenerator defaults() {
        return new SchemaGenerator(null, List.of(MODELS), true);
    }

    private static SchemaGenerator config(boolean mapCommonTypes, boolean ignoreStatic, boolean snakeCase,
                                          boolean version, boolean failOnIssues, String extension) {
        return new SchemaGenerator(new SchemaGenerator.Config(null, List.of(MODELS), true, ignoreStatic, null, List.of(),
                mapCommonTypes, false, snakeCase, version, failOnIssues, extension));
    }

    private String generate(SchemaGenerator generator, String name) throws Exception {
        return generate(generator, name, "test." + name);
    }

    private String generate(SchemaGenerator generator, String name, String namespace) throws Exception {
        File out = tempDir.toFile();
        generator.generate(name, namespace, out);
        return Files.readString(out.toPath().resolve(name + ".fbs"));
    }

    private static String versionLine(String content) {
        return content.lines().filter(l -> l.startsWith("// @version: ")).findFirst().orElseThrow(() -> new AssertionError(content));
    }
}
