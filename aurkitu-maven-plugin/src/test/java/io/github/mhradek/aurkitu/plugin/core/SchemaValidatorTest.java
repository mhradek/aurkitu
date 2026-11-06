package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.FixedArray;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.StringType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Unsupported;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import io.github.mhradek.aurkitu.plugin.core.model.StructDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TableDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Validation rules, checked on hand-built models. */
class SchemaValidatorTest {

    private static final Scalar INT = new Scalar(ScalarKind.INT);
    private static final SchemaType BYTES = new Vector(new Scalar(ScalarKind.UBYTE));

    private static TypeName name(String n) {
        return new TypeName("app", n);
    }

    private static TableDeclaration table(String n, boolean root, Field... fields) {
        return new TableDeclaration(name(n), "java." + n, "", List.of(fields), root);
    }

    private static StructDeclaration struct(String n, Field... fields) {
        return new StructDeclaration(name(n), "java." + n, "", List.of(fields), false);
    }

    private static Field field(String n, SchemaType type, FieldAttribute... attributes) {
        return new Field(n, n, type, "", "", List.of(attributes));
    }

    private static SchemaValidator.Result validate(boolean drop, Declaration... declarations) {
        return new SchemaValidator(drop).validate(new Schema("app", List.of(), null, null, List.of(declarations)));
    }

    private static List<Field> fieldsOf(SchemaValidator.Result result, String n) {
        return result.schema().declarations().stream()
                .filter(d -> d.name().equals(name(n))).map(d -> ((CompositeDeclaration) d).fields())
                .findFirst().orElseThrow();
    }

    @Test
    void validSchemaHasNoIssuesAndIsUnchanged() {
        Schema schema = new Schema("app", List.of(), "ABCD", null, List.of(table("Root", true, field("a", INT))));
        SchemaValidator.Result result = new SchemaValidator(false).validate(schema);
        assertEquals(List.of(), result.issues());
        assertEquals(schema, result.schema());
    }

    @Test
    void unsupportedFieldsAreDropped() {
        var result = validate(false, table("Root", true, field("a", INT), field("c", new Unsupported("char"))));
        assertEquals(List.of("Unsupported field type: Root.c"), result.issues());
        assertEquals(List.of("a"), fieldsOf(result, "Root").stream().map(Field::name).toList());
    }

    @Test
    void partialIdsAreDroppedFromTheWholeTable() {
        var result = validate(false, table("Root", true, field("a", INT, new FieldAttribute.Id(0)), field("b", INT)));
        assertEquals(List.of("Partial field ids on Root; ids omitted (all fields or none must have an id)"), result.issues());
        assertTrue(fieldsOf(result, "Root").stream().noneMatch(f -> f.has(FieldAttribute.Id.class)));
    }

    @Test
    void attributesOnTheWrongTypeAreDropped() {
        var result = validate(false, table("Root", true,
                field("ok", BYTES, new FieldAttribute.NestedFlatbuffer("X"), new FieldAttribute.Flexbuffer(), new FieldAttribute.ForceAlign(8)),
                field("bad", INT, new FieldAttribute.NestedFlatbuffer("X"), new FieldAttribute.Flexbuffer(), new FieldAttribute.ForceAlign(8))));
        assertEquals(List.of(
                "nested_flatbuffer on Root.bad skipped: type must be [ubyte]",
                "flexbuffer on Root.bad skipped: type must be [ubyte]",
                "force_align on Root.bad skipped: only valid on vectors"), result.issues());
        assertEquals(3, fieldsOf(result, "Root").get(0).attributes().size());
        assertEquals(List.of(), fieldsOf(result, "Root").get(1).attributes());
    }

    @Test
    void structsAcceptScalarsEnumsStructsAndFixedArrays() {
        EnumDeclaration color = new EnumDeclaration(name("Color"), "Color", "", ScalarKind.BYTE, List.of(), null);
        var result = validate(false, color, struct("Inner", field("x", INT)),
                struct("Outer",
                        field("x", INT),
                        field("color", new Reference(name("Color"))),
                        field("inner", new Reference(name("Inner"))),
                        field("inners", new FixedArray(new Reference(name("Inner")), 2)),
                        field("external", new Reference(new TypeName("app", "FromInclude"))),
                        field("floats", new FixedArray(new Scalar(ScalarKind.FLOAT), 3))),
                table("Root", true));
        assertEquals(List.of(), result.issues());
    }

    @Test
    void nonScalarStructFieldsAreReportedAndOptionallyDropped() {
        Declaration bad = struct("S", field("ok", INT), field("text", StringType.INSTANCE), field("list", new Vector(INT)));
        var kept = validate(false, bad, table("Root", true));
        assertEquals(List.of(
                "Struct S field text is non-scalar; structs may only contain scalars or other structs",
                "Struct S field list is non-scalar; structs may only contain scalars or other structs"), kept.issues());
        assertEquals(3, fieldsOf(kept, "S").size());

        var dropped = validate(true, bad, table("Root", true));
        assertEquals(List.of("ok"), fieldsOf(dropped, "S").stream().map(Field::name).toList());
    }

    @Test
    void enumValueProblemsAreReported() {
        EnumDeclaration bad = new EnumDeclaration(name("E"), "E", "", ScalarKind.BYTE, List.of(
                new EnumDeclaration.Value("A", 1L), new EnumDeclaration.Value("B", 1L), new EnumDeclaration.Value("C", 500L)), null);
        EnumDeclaration unread = new EnumDeclaration(name("U"), "U", "", ScalarKind.BYTE,
                List.of(new EnumDeclaration.Value("X", null)), "code");
        var result = validate(false, bad, unread, table("Root", true));
        assertEquals(List.of(
                "Enum E has duplicate value 1 (B)",
                "Enum E value 500 (C) is out of range for byte",
                "Enum U: could not read @FlatBufferEnumTypeField code; values are implicit"), result.issues());
    }

    @Test
    void schemaLevelProblemsAreReported() {
        var result = new SchemaValidator(false).validate(new Schema("app", List.of(), "TOOLONG", null, List.of(
                table("A", true), table("A", false), table("B", true))));
        assertEquals(List.of(
                "Duplicate type name app.A (declared by java.A and java.A)",
                "file_identifier \"TOOLONG\" must be exactly 4 characters",
                "Multiple root types; last used: B"), result.issues());

        assertEquals(List.of("No root type defined"), validate(false, table("A", false)).issues());
    }
}
