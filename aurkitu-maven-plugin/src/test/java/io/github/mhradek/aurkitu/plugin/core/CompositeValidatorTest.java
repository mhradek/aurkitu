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
    private static final UnionDeclaration SHAPE = new UnionDeclaration(name("Shape"), "Shape", "", List.of("Root"));

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
        return new SchemaValidator(false).validate(new Schema("app", List.of(), null, null, List.of(declarations)));
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
        assertEquals(1, unionAtZero.issues().size());
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
}
