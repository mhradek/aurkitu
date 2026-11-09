package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Map key types with and without {@code typedMapKeys} ({@code mapkeys} package). */
class MapKeysTest {

    private static final String PACKAGE = "io.github.mhradek.aurkitu.plugin.core.mapkeys";

    @TempDir
    Path tempDir;

    @Test
    void typedKeysKeepTheirTypeAndAreMarkedKey() throws Exception {
        assertEquals(AurkituVersion.schemaHeader() + "\n" + """
                namespace app;

                enum Code : int {
                  LOW = 10,
                  HIGH = 20,
                }

                enum Color : byte {
                  RED,
                  GREEN,
                }

                struct Pos {
                  x:int;
                }

                table Item {
                  id:int;
                }

                table Lookup {
                  by_long:[MapValueSet_Lookup_by_long];
                  by_flag:[MapValueSet_Lookup_by_flag];
                  by_uuid:[MapValueSet_Lookup_by_uuid];
                  by_color:[MapValueSet_Lookup_by_color];
                  by_pos:[MapValueSet_Lookup_by_pos];
                  by_item:[MapValueSet_Lookup_by_item];
                  by_code:[MapValueSet_Lookup_by_code];
                  code_by_name:[MapValueSet_Lookup_code_by_name];
                }

                table MapValueSet_Lookup_by_long {
                  key:long (key);
                  value:int;
                }

                table MapValueSet_Lookup_by_flag {
                  key:bool (key);
                  value:int;
                }

                table MapValueSet_Lookup_by_uuid {
                  key:string (key);
                  value:string;
                }

                table MapValueSet_Lookup_by_color {
                  key:Color (key);
                  value:int;
                }

                table MapValueSet_Lookup_by_pos {
                  key:Pos (key);
                  value:int;
                }

                table MapValueSet_Lookup_by_item {
                  key:Item;
                  value:int;
                }

                table MapValueSet_Lookup_by_code {
                  key:Code = LOW (key);
                  value:int;
                }

                table MapValueSet_Lookup_code_by_name {
                  key:string (key);
                  value:Code = LOW;
                }

                root_type Lookup;

                // Validation issues:
                // - Unsupported field type: Lookup.byList
                // - Unsupported field type: Lookup.byMap
                // - Unsupported field type: Lookup.byBytes
                // - Unsupported field type: Lookup.byObject
                // - Unsupported field type: Lookup.raw
                // - Unsupported field type: Lookup.byWildcard
                // - key on MapValueSet_Lookup_by_item.key skipped: a key must be a string, scalar, enum or struct
                """, generate(true));
    }

    @Test
    void untypedKeysAreAlwaysStringsAsBefore() throws Exception {
        String schema = generate(false);
        assertFalse(schema.contains("(key)"), schema);
        // Only a raw Map, which has no key or value type, is unsupported.
        assertTrue(schema.endsWith("// Validation issues:\n// - Unsupported field type: Lookup.raw\n"), schema);
        // A map value (or key) whose enum has no 0 value gets the lowest value, so flatc accepts the table.
        assertTrue(schema.contains("table MapValueSet_Lookup_code_by_name {\n  key:string;\n  value:Code = LOW;\n}"), schema);
        for (String field : List.of("long", "flag", "uuid", "color", "pos", "item", "list", "map", "bytes", "object", "code", "wildcard")) {
            assertTrue(schema.contains("table MapValueSet_Lookup_by_" + field + " {\n  key:string;\n"), field + ":\n" + schema);
        }
    }

    private String generate(boolean typed) throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(null, List.of(PACKAGE), true).withTypedMapKeys(typed);
        new SchemaGenerator(config).generate("keys", "app", tempDir.toFile());
        return Files.readString(tempDir.resolve("keys.fbs"));
    }
}
