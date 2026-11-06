package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for schema output fixes (enums, type mapping, naming, namespaces, ordering).
 * Models live in the {@code fixes} package.
 */
class SchemaGeneratorFixesTest {

    private static final String PKG = "io.github.mhradek.aurkitu.plugin.core.fixes";

    @TempDir
    static Path tempDir;

    private static String schema;

    @BeforeAll
    static void generate() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null, List.of(PKG), true, true, "FIX1", List.of("other.fbs"),
                true, false, true, false, false, null);
        new SchemaGenerator(config).generate("fixes", "fix.ns", tempDir.toFile());
        schema = Files.readString(tempDir.resolve("fixes.fbs"));
    }

    @Test
    void unsignedEnumKeepsUnderlyingType() {
        assertTrue(schema.contains("enum UnsignedEnum : uint {\n  A,\n  B,\n}"), schema);
    }

    @Test
    void enumTypeFieldValuesAreEmittedInAscendingOrder() {
        assertTrue(schema.contains("enum CodedEnum : short {\n  LOW = -10,\n  MEDIUM = 20,\n  HIGH = 30,\n}"), schema);
    }

    @Test
    void unsignedEnumValuesAreReinterpreted() {
        assertTrue(schema.contains("enum ByteCodedEnum : ubyte {\n  ONE = 1,\n  MAX = 255,\n}"), schema);
    }

    @Test
    void duplicateAndOutOfRangeEnumValuesAreReported() {
        assertTrue(schema.contains("Enum BadEnum has duplicate value 1 (B)"), schema);
        assertTrue(schema.contains("Enum BadEnum value 500 (C) is out of range for byte"), schema);
    }

    @Test
    void jdkTypesMapByActualType() {
        assertTrue(schema.contains("  text:string;"), schema);
        assertTrue(schema.contains("  uuid:string;"), schema);
        assertTrue(schema.contains("  when:string;"), schema);
        assertTrue(schema.contains("  ids:[int];"), schema);
        assertTrue(schema.contains("  names:[string];"), schema);
        assertTrue(schema.contains("  counts:[MapValueSet_Holder_counts];"), schema);
    }

    @Test
    void userTypeNamedLikeACollectionIsAReference() {
        assertTrue(schema.contains("  players:PlayerList;"), schema);
    }

    @Test
    void nestedVectorsAreUnsupported() {
        assertTrue(schema.contains("Unsupported field type: Holder.grid"), schema);
        assertTrue(schema.contains("Unsupported field type: Holder.nested"), schema);
    }

    @Test
    void nestedClassesAreDeclaredAndReferencedBySimpleName() {
        assertTrue(schema.contains("table Inner {"), schema);
        assertTrue(schema.contains("  inner:Inner;"), schema);
        assertFalse(schema.contains("Outer_Inner"), schema);
        assertFalse(schema.contains("Outer$Inner"), schema);
    }

    @Test
    void syntheticFieldsAreSkipped() {
        assertTrue(schema.contains("table Attached {\n  value:int;\n}"), schema);
        assertFalse(schema.contains("this"), schema);
    }

    @Test
    void dollarSignsOutsideNamesArePreserved() {
        assertTrue(schema.contains("// costs $5"), schema);
    }

    @Test
    void namespaceOverrideIsScopedToItsTypeAndQualified() {
        assertTrue(schema.contains("namespace audit.ns;\n\ntable Audit {\n  subject:fix.ns.Holder;\n}\n\nnamespace fix.ns;\n"), schema);
        assertTrue(schema.contains("  audit:audit.ns.Audit;"), schema);
        assertTrue(schema.contains("root_type Holder;"), schema);
    }

    @Test
    void structsAreDeclaredBeforeTheStructsAndTablesThatUseThem() {
        int z = schema.indexOf("struct ZStruct {");
        int a = schema.indexOf("struct AStruct {");
        assertTrue(z >= 0 && a > z, schema);
        assertTrue(a < schema.indexOf("table "), "structs precede tables:\n" + schema);
    }

    @Test
    void structFieldReferencingATableIsReported() {
        assertTrue(schema.contains("Struct StructWithTable field players is non-scalar"), schema);
    }

    @Test
    void includesComeBeforeAllOtherDeclarations() {
        int include = schema.indexOf("include \"other.fbs\";");
        assertTrue(include > 0, schema);
        assertTrue(include < schema.indexOf("namespace fix.ns;"), schema);
    }

    @Test
    void stringIsMappedEvenWithoutCommonTypes() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null, List.of(PKG), true, true, null, List.of(),
                false, false, true, false, false, null);
        Path dir = Files.createDirectories(tempDir.resolve("no-common"));
        new SchemaGenerator(config).generate("nocommon", "fix.ns", dir.toFile());
        String content = Files.readString(dir.resolve("nocommon.fbs"));
        assertTrue(content.contains("  text:string;"), content);
        assertFalse(content.contains("uuid:"), content);
        assertTrue(content.contains("Unsupported field type: Holder.uuid"), content);
    }

    @Test
    void invalidFileIdentifierIsReported() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null, List.of(PKG), true, true, "TOOLONG", List.of(),
                true, false, true, false, false, null);
        Path dir = Files.createDirectories(tempDir.resolve("bad-id"));
        new SchemaGenerator(config).generate("badid", "fix.ns", dir.toFile());
        assertTrue(Files.readString(dir.resolve("badid.fbs")).contains("must be exactly 4 characters"));
    }

    @Test
    void versionHashIgnoresHeaderAndValidationComments() {
        Schema schema = new Schema("v.ns", List.of(), null, null, List.of());
        String a = schema.write("// Aurkitu 1.0.0 header", true, List.of("an issue"));
        String b = schema.write("// Aurkitu 9.9.9 header", true, List.of());
        assertEquals(versionLine(a), versionLine(b));

        Schema other = new Schema("other.ns", List.of(), null, null, List.of());
        assertNotEquals(versionLine(b), versionLine(other.write("// Aurkitu 9.9.9 header", true, List.of())));
    }

    private static String versionLine(String schema) {
        return schema.lines().filter(l -> l.startsWith("// @version: ")).findFirst().orElseThrow();
    }
}
