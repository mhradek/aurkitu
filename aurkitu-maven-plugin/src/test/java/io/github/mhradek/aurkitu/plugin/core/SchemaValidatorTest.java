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
import io.github.mhradek.aurkitu.plugin.core.model.UnionDeclaration;
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

    private static SchemaValidator.Result validate(Declaration... declarations) {
        return new SchemaValidator().validate(new Schema("app", List.of(), null, null, List.of(declarations)));
    }

    /** With an include, references to undeclared types are assumed to come from it. */
    private static SchemaValidator.Result validateWithInclude(Declaration... declarations) {
        return new SchemaValidator().validate(new Schema("app", List.of("other.fbs"), null, null, List.of(declarations)));
    }

    private static List<Field> fieldsOf(SchemaValidator.Result result, String n) {
        return result.schema().declarations().stream()
                .filter(d -> d.name().equals(name(n))).map(d -> ((CompositeDeclaration) d).fields())
                .findFirst().orElseThrow();
    }

    @Test
    void validSchemaHasNoIssuesAndIsUnchanged() {
        Schema schema = new Schema("app", List.of(), "ABCD", null, List.of(table("Root", true, field("a", INT))));
        SchemaValidator.Result result = new SchemaValidator().validate(schema);
        assertEquals(List.of(), result.issues());
        assertEquals(schema, result.schema());
    }

    @Test
    void unsupportedFieldsAreDropped() {
        var result = validate(table("Root", true, field("a", INT), field("c", new Unsupported("char"))));
        assertEquals(List.of("Unsupported field type: Root.c"), result.issues());
        assertEquals(List.of("a"), fieldsOf(result, "Root").stream().map(Field::name).toList());
    }

    @Test
    void partialIdsAreDroppedFromTheWholeTable() {
        var result = validate(table("Root", true, field("a", INT, new FieldAttribute.Id(0)), field("b", INT)));
        assertEquals(List.of("Partial field ids on Root; ids omitted (all fields or none must have an id)"), result.issues());
        assertTrue(fieldsOf(result, "Root").stream().noneMatch(f -> f.has(FieldAttribute.Id.class)));
    }

    @Test
    void attributesOnTheWrongTypeAreDropped() {
        var result = validate(table("X", false), table("Root", true,
                field("ok", BYTES, new FieldAttribute.NestedFlatbuffer("X"), new FieldAttribute.Flexbuffer(), new FieldAttribute.ForceAlign(8)),
                field("bad", INT, new FieldAttribute.NestedFlatbuffer("X"), new FieldAttribute.Flexbuffer(), new FieldAttribute.ForceAlign(8))));
        assertEquals(List.of(
                "nested_flatbuffer on Root.bad skipped: type must be [ubyte]",
                "flexbuffer on Root.bad skipped: type must be [ubyte]"), result.issues());
        // flatc accepts force_align on a non-vector and ignores it: a warning, and left out.
        assertEquals(List.of("force_align on Root.bad has no effect: only vectors are aligned (align a struct with"
                + " @FlatBufferTable(forceAlign)); left out"), result.warnings());
        assertEquals(3, fieldsOf(result, "Root").get(0).attributes().size());
        assertEquals(List.of(), fieldsOf(result, "Root").get(1).attributes());
    }

    @Test
    void structsAcceptScalarsEnumsStructsAndFixedArrays() {
        EnumDeclaration color = new EnumDeclaration(name("Color"), "Color", "", ScalarKind.BYTE,
                List.of(new EnumDeclaration.Value("RED", null)), null);
        var result = validateWithInclude(color, struct("Inner", field("x", INT)),
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
    void nonScalarStructFieldsAreReportedAndDropped() {
        Declaration bad = struct("S", field("ok", INT), field("text", StringType.INSTANCE), field("list", new Vector(INT)));
        var result = validate(bad, table("Root", true));
        assertEquals(List.of(
                "Struct S field text is non-scalar; structs may only contain scalars or other structs",
                "Struct S field list is non-scalar; structs may only contain scalars or other structs"), result.issues());
        assertEquals(List.of("ok"), fieldsOf(result, "S").stream().map(Field::name).toList());
    }

    @Test
    void structFieldsReferencingTablesOrUnionsAreNonScalar() {
        var result = validate(
                UnionDeclaration.of(name("Any"), "Any", "", List.of("Root")),
                struct("S", field("table", new Reference(name("Root"))), field("union", new Reference(name("Any")))),
                table("Root", true));
        assertEquals(List.of(
                "Struct S field table is non-scalar; structs may only contain scalars or other structs",
                "Struct S field union is non-scalar; structs may only contain scalars or other structs",
                "Struct S has no valid fields and is left out (flatc does not allow empty structs)"), result.issues());
    }

    @Test
    void emptyStructsAreLeftOutWithTheFieldsAndStructsThatUseThem() {
        var result = validate(
                struct("Empty", field("text", StringType.INSTANCE)),
                struct("Outer", field("inner", new Reference(name("Empty")))),
                struct("Kept", field("x", INT), field("inner", new Reference(name("Empty")))),
                table("Root", true, field("outer", new Reference(name("Outer"))), field("kept", new Reference(name("Kept")))));
        assertEquals(List.of(
                "Struct Empty field text is non-scalar; structs may only contain scalars or other structs",
                "Struct Empty has no valid fields and is left out (flatc does not allow empty structs)",
                "Field Outer.inner references app.Empty, which is left out; field left out",
                "Struct Outer has no valid fields and is left out (flatc does not allow empty structs)",
                "Field Kept.inner references app.Empty, which is left out; field left out",
                "Field Root.outer references app.Outer, which is left out; field left out"), result.issues());
        assertEquals(List.of("Kept", "Root"), result.schema().declarations().stream().map(d -> d.name().name()).toList());
        assertEquals(List.of("x"), fieldsOf(result, "Kept").stream().map(Field::name).toList());
        assertEquals(List.of("kept"), fieldsOf(result, "Root").stream().map(Field::name).toList());
    }

    @Test
    void fieldsReferencingUndeclaredTypesAreLeftOutUnlessAnIncludeMayDeclareThem() {
        Field dangling = field("other", new Reference(name("Missing")));
        assertEquals(List.of(), fieldsOf(validate(table("Root", true, dangling)), "Root"));
        assertEquals(List.of(dangling), fieldsOf(validateWithInclude(table("Root", true, dangling)), "Root"));
    }

    @Test
    void rootTypeMustBeATable() {
        StructDeclaration root = new StructDeclaration(name("S"), "java.S", "", List.of(field("x", INT)), true);
        var result = validate(root);
        assertEquals(List.of("Root type S is a struct; root_type must be a table"), result.issues());
        assertTrue(result.schema().root().isEmpty(), "struct root is not written");
    }

    @Test
    void tableEnumFieldsNeedADefaultThatIsAnEnumValue() {
        EnumDeclaration implicit = new EnumDeclaration(name("Color"), "Color", "", ScalarKind.BYTE,
                List.of(new EnumDeclaration.Value("RED", null), new EnumDeclaration.Value("GREEN", null)), null);
        EnumDeclaration noZero = new EnumDeclaration(name("Status"), "Status", "", ScalarKind.UINT,
                List.of(new EnumDeclaration.Value("ACTIVE", 1L), new EnumDeclaration.Value("CLOSED", 10L)), null);
        Reference color = new Reference(name("Color"));
        Reference status = new Reference(name("Status"));
        var result = validate(implicit, noZero, table("Root", true,
                field("a", color),                                         // implicit values include 0: ok
                new Field("b", "b", color, "GREEN", "", List.of()),          // by name: ok
                new Field("c", "c", status, "10", "", List.of()),            // by value: ok
                field("d", status),                                         // no 0, no default
                new Field("e", "e", color, "PURPLE", "", List.of()),         // not a member; 0 is, so it can go
                field("f", new Vector(status)),                             // vectors have no default: ok
                new Field("g", "g", status, "0", "", List.of())));           // not a member; nothing valid without it
        assertEquals(List.of(
                "Field Root.d needs a defaultValue: enum Status has no value 0, which is the default otherwise",
                "Field Root.e default PURPLE is not a value of enum Color; default left out",
                "Field Root.g default 0 is not a value of enum Status, which has no value 0: set a defaultValue that is"
                        + " one of its values"), result.issues());
        assertEquals("", fieldsOf(result, "Root").get(4).defaultValue(), "e: left out, so the field defaults to RED (0)");
        assertEquals("0", fieldsOf(result, "Root").get(6).defaultValue(), "g: kept for the user to fix");
    }

    @Test
    void structEnumFieldsNeedNoDefault() {
        EnumDeclaration noZero = new EnumDeclaration(name("Status"), "Status", "", ScalarKind.UINT,
                List.of(new EnumDeclaration.Value("ACTIVE", 1L)), null);
        var result = validate(noZero, struct("S", field("s", new Reference(name("Status")))), table("Root", true));
        assertEquals(List.of(), result.issues());
    }

    @Test
    void referencesToUndeclaredTypesAreReportedWithoutIncludes() {
        Field dangling = field("other", new Vector(new Reference(name("Missing"))));
        assertEquals(List.of("Field Root.other references app.Missing, which is not declared in this schema;"
                        + " annotate it, or include the schema that declares it"),
                validate(table("Root", true, dangling)).issues());
        assertEquals(List.of(), validateWithInclude(table("Root", true, dangling)).issues());
    }

    @Test
    void unionMembersMustBeDeclaredTablesOrStructs() {
        EnumDeclaration color = new EnumDeclaration(name("Color"), "Color", "", ScalarKind.BYTE,
                List.of(new EnumDeclaration.Value("RED", null)), null);
        UnionDeclaration union = UnionDeclaration.of(name("Any"), "Any", "", List.of("Root", "S", "Color", "Gone"));
        Declaration[] declarations = {color, union, struct("S", field("x", INT)), table("Root", true)};
        assertEquals(List.of(
                "Union Any member Color is not a table",
                "Union Any member Gone is not declared in this schema"),
                validate(declarations).issues());
        assertEquals(List.of("Union Any member Color is not a table"), validateWithInclude(declarations).issues());
        assertEquals(List.of("Union None has no members"),
                validate(UnionDeclaration.of(name("None"), "None", "", List.of()), table("Root", true)).issues());
    }

    @Test
    void emptyEnumsAreReported() {
        EnumDeclaration empty = new EnumDeclaration(name("E"), "E", "", ScalarKind.BYTE, List.of(), null);
        assertEquals(List.of("Enum E has no values"), validate(empty, table("Root", true)).issues());
    }

    @Test
    void namespacesMustBeDottedIdentifiers() {
        TableDeclaration bad = new TableDeclaration(new TypeName("audit-log", "A"), "A", "", List.of(), false);
        var result = new SchemaValidator().validate(new Schema("my.app2", List.of(), null, null,
                List.of(table("Root", true), bad)));
        assertEquals(List.of("Namespace \"audit-log\" is not valid: use dot-separated identifiers (letters, digits, _)"),
                result.issues());
        assertEquals(1, new SchemaValidator().validate(new Schema("2fast", List.of(), null, null,
                List.of(new TableDeclaration(new TypeName("2fast", "R"), "R", "", List.of(), true)))).issues().size());
    }

    @Test
    void enumValueProblemsAreReported() {
        EnumDeclaration bad = new EnumDeclaration(name("E"), "E", "", ScalarKind.BYTE, List.of(
                new EnumDeclaration.Value("A", 1L), new EnumDeclaration.Value("B", 1L), new EnumDeclaration.Value("C", 500L)), null);
        EnumDeclaration unread = new EnumDeclaration(name("U"), "U", "", ScalarKind.BYTE,
                List.of(new EnumDeclaration.Value("X", null)), "could not read @FlatBufferEnumTypeField code");
        var result = validate(bad, unread, table("Root", true));
        assertEquals(List.of(
                "Enum E has duplicate value 1 (B)",
                "Enum E value 500 (C) is out of range for byte",
                "Enum U: could not read @FlatBufferEnumTypeField code; values are implicit"), result.issues());
    }

    @Test
    void schemaLevelProblemsAreReported() {
        var result = new SchemaValidator().validate(new Schema("app", List.of(), "TOOLONG", null, List.of(
                table("A", true), table("A", false), table("B", true))));
        assertEquals(List.of(
                "Duplicate type name app.A (declared by java.A and java.A)",
                "file_identifier \"TOOLONG\" must be exactly 4 characters",
                "Multiple root types; last used: B"), result.issues());

        assertEquals(List.of("No root type defined"), validate(table("A", false)).issues());
    }
}
