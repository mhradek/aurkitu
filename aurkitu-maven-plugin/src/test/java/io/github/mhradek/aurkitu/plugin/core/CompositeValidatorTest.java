package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.RpcServiceDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.FixedArray;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.StringType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import io.github.mhradek.aurkitu.plugin.core.model.StructDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TableDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;
import io.github.mhradek.aurkitu.plugin.core.model.UnionDeclaration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Field rules from the FlatBuffers schema documentation, checked on hand-built models. */
class CompositeValidatorTest {

    private static final Scalar INT = new Scalar(ScalarKind.INT);
    private static final EnumDeclaration COLOR = new EnumDeclaration(name("Color"), "Color", "", ScalarKind.BYTE,
            List.of(new EnumDeclaration.Value("RED", null), new EnumDeclaration.Value("GREEN", null)), null);
    private static final UnionDeclaration SHAPE = UnionDeclaration.of(name("Shape"), "Shape", "", List.of("Root"));

    private static TypeName name(String n) {
        return new TypeName("app", n);
    }

    private static Field field(String n, SchemaType type, String defaultValue, FieldAttribute... attributes) {
        return new Field(n, n, type, defaultValue, "", List.of(attributes));
    }

    private static Field field(String n, SchemaType type, FieldAttribute... attributes) {
        return field(n, type, "", attributes);
    }

    private static TableDeclaration root(Field... fields) {
        return new TableDeclaration(name("Root"), "java.Root", "", List.of(fields), true);
    }

    private static SchemaValidator.Result validate(Declaration... declarations) {
        return new SchemaValidator().validate(new Schema("app", List.of(), null, null, List.of(declarations)));
    }

    private static Field fieldOf(SchemaValidator.Result result, String declaration, String field) {
        return result.schema().declarations().stream()
                .filter(d -> d.name().equals(name(declaration)))
                .flatMap(d -> ((CompositeDeclaration) d).fields().stream())
                .filter(f -> f.javaName().equals(field)).findFirst().orElseThrow();
    }

    @Test
    void requiredOnlyOnNonScalarTableFields() {
        var result = validate(COLOR, root(
                field("text", StringType.INSTANCE, new FieldAttribute.Required()),
                field("count", INT, new FieldAttribute.Required()),
                field("color", new Reference(name("Color")), new FieldAttribute.Required())),
                new StructDeclaration(name("S"), "S", "", List.of(field("x", INT, new FieldAttribute.Required())), false));
        assertEquals(List.of(
                "required on Root.count skipped: only strings, vectors, tables and structs can be required",
                "required on Root.color skipped: only strings, vectors, tables and structs can be required",
                "required on S.x skipped: struct fields are always present"), result.issues());
        assertTrue(fieldOf(result, "Root", "text").has(FieldAttribute.Required.class));
        assertFalse(fieldOf(result, "Root", "count").has(FieldAttribute.Required.class));
    }

    @Test
    void defaultsOnlyOnScalarsAndEnumsInTablesAndNotWithRequired() {
        var result = validate(root(
                field("text", StringType.INSTANCE, "hello"),
                field("name", StringType.INSTANCE, "x", new FieldAttribute.Required()),
                field("list", new Vector(INT), "null")),
                new StructDeclaration(name("S"), "S", "", List.of(field("x", INT, "5")), false));
        assertEquals(List.of(
                "default hello on Root.text skipped: only scalar and enum fields can have defaults",
                "default x on Root.name skipped: a required field cannot have a default",
                "default null on Root.list skipped: only scalar and enum fields can have defaults",
                "default 5 on S.x skipped: struct fields cannot have defaults"), result.issues());
        assertEquals("", fieldOf(result, "Root", "text").defaultValue());
        assertEquals("", fieldOf(result, "S", "x").defaultValue());
    }

    @Test
    void scalarDefaultsMustFitTheirType() {
        var result = validate(root(
                field("a", new Scalar(ScalarKind.UBYTE), "300"),
                field("b", INT, "abc"),
                field("c", new Scalar(ScalarKind.BOOL), "yes"),
                field("d", new Scalar(ScalarKind.FLOAT), "1.5x"),
                field("ok1", new Scalar(ScalarKind.UBYTE), "0xFF"),
                field("ok2", new Scalar(ScalarKind.BYTE), "-128"),
                field("ok3", new Scalar(ScalarKind.DOUBLE), "-inf"),
                field("ok4", new Scalar(ScalarKind.FLOAT), "3.e4"),
                field("ok5", new Scalar(ScalarKind.BOOL), "true"),
                field("ok6", INT, "null")));
        assertEquals(List.of(
                "default 300 on Root.a skipped: out of range for ubyte",
                "default abc on Root.b skipped: not an integer",
                "default yes on Root.c skipped: not a bool (true or false)",
                "default 1.5x on Root.d skipped: not a number"), result.issues());
        assertEquals("null", fieldOf(result, "Root", "ok6").defaultValue());
    }

    @Test
    void optionalEnumsAreValid() {
        // Regression: "= null" was reported as "not a value of enum".
        var result = validate(COLOR, root(field("color", new Reference(name("Color")), "null")));
        assertEquals(List.of(), result.issues());
    }

    @Test
    void structFieldsCannotBeDeprecated() {
        var result = validate(root(), new StructDeclaration(name("S"), "S", "",
                List.of(field("x", INT, new FieldAttribute.Deprecated())), false));
        assertEquals(List.of("deprecated on S.x skipped: struct fields cannot be deprecated"), result.issues());
    }

    @Test
    void fixedLengthArraysOutsideStructsBecomeVectors() {
        var result = validate(root(field("values", new FixedArray(INT, 3))));
        assertEquals(List.of("fixedLength on Root.values ignored: fixed-length arrays are only allowed in structs;"
                + " written as a vector"), result.issues());
        assertEquals(new Vector(INT), fieldOf(result, "Root", "values").type());
    }

    @Test
    void idsMustBeUniqueAndContiguousWithUnionsTakingTwo() {
        var valid = validate(SHAPE, root(
                field("a", INT, new FieldAttribute.Id(0)),
                field("shape", new Reference(name("Shape")), new FieldAttribute.Id(2))));
        assertEquals(List.of(), valid.issues());

        var invalid = validate(SHAPE, root(
                field("a", INT, new FieldAttribute.Id(0)),
                field("b", INT, new FieldAttribute.Id(0)),
                field("c", INT, new FieldAttribute.Id(5))));
        assertEquals(List.of("Field ids on Root must be unique and contiguous from 0, and a union field also takes"
                + " the id before its own; ids omitted (used: [0, 5])"), invalid.issues());
        assertFalse(fieldOf(invalid, "Root", "a").has(FieldAttribute.Id.class));

        var unionAtZero = validate(SHAPE, root(field("shape", new Reference(name("Shape")), new FieldAttribute.Id(0))));
        assertEquals(List.of("Field ids on Root must be unique and contiguous from 0, and a union field also takes"
                + " the id before its own; ids omitted (used: [0])"), unionAtZero.issues());

        // Duplicates alone, with no gap, are caught too.
        var duplicate = validate(root(
                field("a", INT, new FieldAttribute.Id(0)),
                field("b", INT, new FieldAttribute.Id(1)),
                field("c", INT, new FieldAttribute.Id(1))));
        assertEquals(List.of("Field ids on Root must be unique and contiguous from 0, and a union field also takes"
                + " the id before its own; ids omitted (used: [0, 1])"), duplicate.issues());
    }

    @Test
    void structFixedArraysHoldOnlyScalarsAndStructsAndAreNotNested() {
        StructDeclaration struct = new StructDeclaration(name("S"), "S", "", List.of(
                field("ints", new FixedArray(INT, 2)),
                field("texts", new FixedArray(StringType.INSTANCE, 2)),
                field("grid", new FixedArray(new FixedArray(INT, 2), 2)),
                field("missing", new FixedArray(new Reference(name("Missing")), 2))), false);
        assertEquals(List.of(
                "Struct S field texts is non-scalar; structs may only contain scalars or other structs",
                "Struct S field grid is non-scalar; structs may only contain scalars or other structs",
                "Field S.missing references app.Missing, which is not declared in this schema; annotate it,"
                        + " or include the schema that declares it"), validate(struct, root()).issues());
    }

    @Test
    void oneKeyOfAStringScalarOrEnum() {
        var result = validate(COLOR, root(
                field("list", new Vector(INT), new FieldAttribute.Key()),
                field("name", StringType.INSTANCE, new FieldAttribute.Key()),
                field("count", INT, new FieldAttribute.Key())));
        assertEquals(List.of(
                "key on Root.list skipped: a key must be a string, scalar or enum",
                "key on Root.count skipped: only one key is allowed (already on name)"), result.issues());
        assertTrue(fieldOf(result, "Root", "name").has(FieldAttribute.Key.class));
    }

    @Test
    void hashNeedsAKnownAlgorithmOnAnIntegerOfItsWidth() {
        var result = validate(root(
                field("a", new Scalar(ScalarKind.UINT), new FieldAttribute.Hash("fnv1a_32")),
                field("b", new Scalar(ScalarKind.ULONG), new FieldAttribute.Hash("fnv1_64")),
                field("c", StringType.INSTANCE, new FieldAttribute.Hash("fnv1a_32")),
                field("d", new Scalar(ScalarKind.LONG), new FieldAttribute.Hash("fnv1a_32")),
                field("e", INT, new FieldAttribute.Hash("md5"))));
        assertEquals(List.of(
                "hash on Root.c skipped: fnv1a_32 needs a 32-bit integer field",
                "hash on Root.d skipped: fnv1a_32 needs a 32-bit integer field",
                "hash on Root.e skipped: md5 is not one of [fnv1_32, fnv1_64, fnv1a_32, fnv1a_64]"), result.issues());
    }

    @Test
    void nestedFlatbufferNamesADeclaredTable() {
        SchemaType bytes = new Vector(new Scalar(ScalarKind.UBYTE));
        var result = validate(root(
                field("ok", bytes, new FieldAttribute.NestedFlatbuffer("Root")),
                field("qualified", bytes, new FieldAttribute.NestedFlatbuffer("app.Root")),
                field("missing", bytes, new FieldAttribute.NestedFlatbuffer("Nope"))));
        assertEquals(List.of("nested_flatbuffer on Root.missing skipped: Nope is not a table declared in this schema"),
                result.issues());
    }

    @Test
    void fieldNamesMustBeUniqueIncludingHiddenUnionTypeFields() {
        var result = validate(SHAPE, root(
                new Field("fooBar", "foo_bar", StringType.INSTANCE, "", "", List.of()),
                new Field("foo_bar", "foo_bar", StringType.INSTANCE, "", "", List.of()),
                field("shape", new Reference(name("Shape"))),
                field("shape_type", StringType.INSTANCE)));
        assertEquals(List.of(
                "Fields Root.fooBar and Root.foo_bar are both written as foo_bar",
                "Field Root.shape_type clashes with the hidden shape_type field flatc adds for union field shape"),
                result.issues());
    }

    @Test
    void vectorsOfUnionsAreReported() {
        var result = validate(SHAPE, root(field("shapes", new Vector(new Reference(name("Shape"))))));
        assertEquals(List.of("Field Root.shapes is a vector of unions, which flatc supports only for C++"), result.issues());
    }

    @Test
    void inheritedFieldsWithoutIdsAreAWarningNotAnIssue() {
        Field own = new Field("own", "own", INT, "", "", List.of(), "java.Root");
        Field inherited = new Field("base", "base", INT, "", "", List.of(), "java.Base");
        var result = validate(root(own, inherited));
        assertEquals(List.of(), result.issues());
        assertEquals(List.of("Table Root has inherited fields (from Base) after its own; adding a field to Root moves"
                + " them and breaks existing data. Give every field a fieldId to fix their positions."), result.warnings());

        var withIds = validate(root(own.withAttributes(List.of(new FieldAttribute.Id(0))),
                inherited.withAttributes(List.of(new FieldAttribute.Id(1)))));
        assertEquals(List.of(), withIds.warnings());
    }

    @Test
    void typeAttributesBelongToTheRightKindOfType() {
        var result = validate(
                new TableDeclaration(name("Root"), "Root", "", List.of(), true,
                        List.of(new FieldAttribute.OriginalOrder(), new FieldAttribute.ForceAlign(16))),
                new StructDeclaration(name("S"), "S", "", List.of(field("x", INT)), false,
                        List.of(new FieldAttribute.OriginalOrder(), new FieldAttribute.ForceAlign(16))),
                new StructDeclaration(name("T"), "T", "", List.of(field("x", INT)), false,
                        List.of(new FieldAttribute.ForceAlign(12))));
        assertEquals(List.of(
                "force_align on table Root skipped: only structs (on a table, put it on a vector field)",
                "original_order on struct S skipped: only tables",
                "force_align on struct T skipped: 12 is not a power of two"), result.issues());
        assertEquals(List.of(new FieldAttribute.OriginalOrder()),
                ((CompositeDeclaration) result.schema().declarations().get(0)).attributes());
    }

    @Test
    void customAttributesNeedAValidNameThatIsNotBuiltIn() {
        var result = validate(COLOR.withAttributes(List.of(new FieldAttribute.Custom("1st", ""))),
                SHAPE.withAttributes(List.of(new FieldAttribute.Custom("help", "\"x\""))),
                new TableDeclaration(name("Root"), "Root", "", List.of(
                        field("a", INT, new FieldAttribute.Custom("priority", "1"), new FieldAttribute.Custom("key", ""))),
                        true, List.of(new FieldAttribute.Custom("bad name", ""))));
        assertEquals(List.of(
                "attribute \"1st\" on enum Color skipped: not a valid name",
                "attribute key on Root.a skipped: built in; use the annotation option instead",
                "attribute \"bad name\" on table Root skipped: not a valid name"), result.issues());
        assertEquals(List.of("help", "priority"), result.schema().customAttributeNames()); // order of first use
    }

    @Test
    void bitFlagsNeedAnUnsignedTypeAndValuesThatFit() {
        EnumDeclaration signed = new EnumDeclaration(name("Signed"), "Signed", "", ScalarKind.BYTE,
                List.of(new EnumDeclaration.Value("A", null)), null, List.of(new FieldAttribute.BitFlags()));
        EnumDeclaration tooBig = new EnumDeclaration(name("Big"), "Big", "", ScalarKind.UBYTE,
                List.of(new EnumDeclaration.Value("A", 3L), new EnumDeclaration.Value("B", 8L)), null,
                List.of(new FieldAttribute.BitFlags()));
        var result = validate(signed, tooBig, root());
        assertEquals(List.of(
                "bit_flags on enum Signed skipped: needs an unsigned enumType (UBYTE, USHORT, UINT, ULONG)",
                "Enum Big value B is bit 8, but bit_flags on ubyte allows bits 0 to 7"), result.issues());
        assertFalse(((EnumDeclaration) result.schema().declarations().get(0)).bitFlags());
        assertTrue(((EnumDeclaration) result.schema().declarations().get(1)).bitFlags());
    }

    @Test
    void bitFlagsMayUseBitZeroAndTheHighestBit() {
        EnumDeclaration implicit = new EnumDeclaration(name("Implicit"), "Implicit", "", ScalarKind.UBYTE,
                List.of(new EnumDeclaration.Value("A", null), new EnumDeclaration.Value("B", null)), null,
                List.of(new FieldAttribute.BitFlags()));
        EnumDeclaration edges = new EnumDeclaration(name("Edges"), "Edges", "", ScalarKind.UBYTE,
                List.of(new EnumDeclaration.Value("LOW", 0L), new EnumDeclaration.Value("HIGH", 7L)), null,
                List.of(new FieldAttribute.BitFlags()));
        assertEquals(List.of(), validate(implicit, edges, root()).issues());
    }

    @Test
    void bitFlagsFieldsMayDefaultToNoFlags() {
        EnumDeclaration flags = new EnumDeclaration(name("Flags"), "Flags", "", ScalarKind.UBYTE,
                List.of(new EnumDeclaration.Value("A", 1L), new EnumDeclaration.Value("B", 2L)), null,
                List.of(new FieldAttribute.BitFlags()));
        assertEquals(List.of(), validate(flags, root(field("flags", new Reference(name("Flags"))))).issues());
    }

    @Test
    void unionMemberNamesMustBeUniqueAndNotNone() {
        UnionDeclaration union = new UnionDeclaration(name("U"), "U", "", List.of(
                new UnionDeclaration.Member("NONE", name("Root"), ""),
                new UnionDeclaration.Member("Twice", name("Root"), ""),
                new UnionDeclaration.Member("Twice", name("Root"), "")));
        assertEquals(List.of(
                "Union U member NONE: the name is reserved (flatc adds NONE for no value)",
                "Union U has two members named Twice"), validate(union, root()).issues());
    }

    @Test
    void rpcMethodsTakeAndReturnDeclaredTables() {
        RpcServiceDeclaration service = new RpcServiceDeclaration(name("Svc"), "Svc", "", List.of(
                new RpcServiceDeclaration.Method("ok", name("Root"), name("Root"), ""),
                new RpcServiceDeclaration.Method("noArgs", null, name("Root"), ""),
                new RpcServiceDeclaration.Method("enumReply", name("Root"), name("Color"), ""),
                new RpcServiceDeclaration.Method("missing", name("Gone"), name("Root"), "")));
        var result = validate(COLOR, root(), service);
        assertEquals(List.of(
                "RPC method Svc.noArgs skipped: it must take exactly one table and return a table",
                "RPC method Svc.enumReply uses app.Color, which is not a table",
                "RPC method Svc.missing uses app.Gone, which is not declared in this schema"), result.issues());
        assertEquals(3, ((RpcServiceDeclaration) result.schema().declarations().get(2)).methods().size());

        var empty = validate(root(), new RpcServiceDeclaration(name("None"), "None", "", List.of()));
        assertEquals(List.of("RPC service None has no methods"), empty.issues());
    }
}
