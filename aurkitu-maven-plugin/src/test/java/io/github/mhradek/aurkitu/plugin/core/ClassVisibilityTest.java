package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Types are scanned whatever their visibility ({@code visibility} package). Before 1.1.1 only public
 * classes were: a package-private annotated type was not declared (and fields using it were left out),
 * a package-private superclass's fields were lost, and a private nested type was written empty.
 */
class ClassVisibilityTest {

    @TempDir
    Path tempDir;

    @Test
    void nonPublicTypesSuperclassesAndNestedTypesAreWritten() throws Exception {
        new SchemaGenerator(null, List.of("io.github.mhradek.aurkitu.plugin.core.visibility"), true)
                .generate("vis", "app", tempDir.toFile());
        String schema = Files.readString(tempDir.resolve("vis.fbs"));
        assertEquals(AurkituVersion.schemaHeader() + "\n" + """
                namespace app;

                enum Kind : byte {
                  RETAIL,
                  WHOLESALE,
                }

                table Hidden {
                  secret:int;
                }

                table Order {
                  id:long;
                  hidden:Hidden;
                  kind:Kind;
                  line:Line;
                  note:Note;
                  created_by:long;
                }

                table Line {
                  qty:int;
                }

                table Note {
                  text:string;
                }

                root_type Order;

                // Warnings:
                // - Table Order has inherited fields (from Base) after its own; adding a field to Order moves them\
                 and breaks existing data. Give every field a fieldId to fix their positions.
                """, schema);
    }
}
