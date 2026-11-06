package io.github.mhradek.aurkitu.plugin.core.model;

import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.FixedArray;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.StringType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Unsupported;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Each model element writes its own syntax. */
class SchemaModelTest {

    private static final TypeName ITEM = new TypeName("app", "Item");
    private static final TypeName AUDIT = new TypeName("app.audit", "Audit");

    @Test
    void typeNamesAreQualifiedOnlyOutsideTheirNamespace() {
        assertEquals("Item", ITEM.referenceFrom("app"));
        assertEquals("app.Item", ITEM.referenceFrom("other"));
        assertEquals("Bare", new TypeName("", "Bare").qualified());
    }

    @Test
    void typesWriteTheirSyntax() {
        assertEquals("uint", new Scalar(ScalarKind.UINT).write("app"));
        assertEquals("string", StringType.INSTANCE.write("app"));
        assertEquals("[Item]", new Vector(new Reference(ITEM)).write("app"));
        assertEquals("[app.Item]", new Vector(new Reference(ITEM)).write("app.audit"));
        assertEquals("[float:4]", new FixedArray(new Scalar(ScalarKind.FLOAT), 4).write("app"));
        assertEquals("app.Item", new Reference(ITEM, true).write("app"));
        assertThrows(IllegalStateException.class, () -> new Unsupported("char").write("app"));
    }

    @Test
    void byteVectorsAreRecognisedByType() {
        assertTrue(SchemaType.isByteVector(new Vector(new Scalar(ScalarKind.UBYTE))));
        assertTrue(SchemaType.isByteVector(new Vector(new Scalar(ScalarKind.BYTE))));
        assertFalse(SchemaType.isByteVector(new Vector(new Scalar(ScalarKind.INT))));
        assertFalse(SchemaType.isByteVector(new Scalar(ScalarKind.UBYTE)));
    }

    @Test
    void scalarKindsKnowTheirRanges() {
        assertTrue(ScalarKind.UBYTE.contains(BigInteger.valueOf(255)));
        assertFalse(ScalarKind.UBYTE.contains(BigInteger.valueOf(256)));
        assertFalse(ScalarKind.BYTE.contains(BigInteger.valueOf(-129)));
        assertTrue(ScalarKind.ULONG.contains(new BigInteger("18446744073709551615")));
        assertTrue(ScalarKind.UINT.isUnsigned());
        assertFalse(ScalarKind.INT.isUnsigned());
        assertFalse(ScalarKind.FLOAT.isIntegral());
        assertFalse(ScalarKind.FLOAT.contains(BigInteger.ZERO));
    }

    @Test
    void fieldWritesCommentDefaultAndAttributesInOrder() {
        Field field = new Field("hp", "hp", new Scalar(ScalarKind.INT), "100", "Starting health",
                List.of(new FieldAttribute.Required(), new FieldAttribute.Id(3), new FieldAttribute.Hash("fnv1a_32")));
        SchemaWriter out = new SchemaWriter("app");
        field.write(out);
        assertEquals("  // Starting health\n  hp:int = 100 (required, id: 3, hash: \"fnv1a_32\");\n", out.text());
    }

    @Test
    void everyAttributeWritesItself() {
        assertEquals("deprecated", new FieldAttribute.Deprecated().write());
        assertEquals("key", new FieldAttribute.Key().write());
        assertEquals("nested_flatbuffer: \"Item\"", new FieldAttribute.NestedFlatbuffer("Item").write());
        assertEquals("flexbuffer", new FieldAttribute.Flexbuffer().write());
        assertEquals("force_align: 16", new FieldAttribute.ForceAlign(16).write());
    }

    @Test
    void enumsAndUnionsWriteThemselves() {
        SchemaWriter out = new SchemaWriter("app");
        new EnumDeclaration(new TypeName("app", "Big"), "Big", "", ScalarKind.ULONG,
                List.of(new EnumDeclaration.Value("MAX", -1L)), null).write(out);
        new UnionDeclaration(new TypeName("app", "Any"), "Any", "Either", List.of("A", "B")).write(out);
        assertEquals("enum Big : ulong {\n  MAX = 18446744073709551615,\n}\n\n"
                + "// Either\nunion Any {\n  A,\n  B,\n}\n\n", out.text());
    }

    @Test
    void schemaPlacesIncludesFirstAndScopesNamespaceOverrides() {
        TableDeclaration audit = new TableDeclaration(AUDIT, "Audit", "", List.of(Field.of("item", new Reference(ITEM))), false);
        TableDeclaration item = new TableDeclaration(ITEM, "Item", "", List.of(Field.of("audit", new Reference(AUDIT))), true);
        Schema schema = new Schema("app", List.of("other.fbs"), "ABCD", "bin", List.of(audit, item));

        assertEquals("""
                include "other.fbs";

                namespace app;

                file_identifier "ABCD";

                file_extension "bin";

                namespace app.audit;

                table Audit {
                  item:app.Item;
                }

                namespace app;

                table Item {
                  audit:app.audit.Audit;
                }

                root_type Item;
                """, schema.writeBody());
    }

    @Test
    void headerVersionAndIssuesWrapTheBody() {
        Schema schema = new Schema("app", List.of(), null, null, List.of());
        String body = schema.writeBody();
        assertEquals("// header\n// @version: " + Schema.version(body) + "\n" + body
                        + "\n// Validation issues:\n// - one\n",
                schema.write("// header", true, List.of("one")));
        assertEquals("// header\n" + body, schema.write("// header", false, List.of()));
    }

    @Test
    void rootIsTheLastCompositeMarkedRoot() {
        TableDeclaration a = new TableDeclaration(new TypeName("app", "A"), "A", "", List.of(), true);
        StructDeclaration b = new StructDeclaration(new TypeName("app", "B"), "B", "", List.of(), true);
        assertSame(b, new Schema("app", List.of(), null, null, List.of(a, b)).root().orElseThrow());
        assertTrue(new Schema("app", List.of(), null, null, List.of()).root().isEmpty());
    }
}
