package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Map values of enum types ({@code mapenums} package). flatc requires a table enum field's default to be one
 * of its values, and the generated map fields cannot be annotated, so an enum without a 0 value gets its lowest
 * value as default; an enum with a 0 value, and a bit_flags enum (0 is "no flags"), get none.
 */
class MapEnumDefaultsTest {

    @TempDir
    Path tempDir;

    @Test
    void mapEnumValuesWithoutZeroDefaultToTheirLowestValue() throws Exception {
        new SchemaGenerator(List.of("io.github.mhradek.aurkitu.plugin.core.mapenums"), true)
                .generate("enums", "app", tempDir.toFile());
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

                enum Perm : ubyte (bit_flags) {
                  READ = 1,
                  WRITE = 2,
                }

                table Holder {
                  codes:[MapValueSet_Holder_codes];
                  colors:[MapValueSet_Holder_colors];
                  perms:[MapValueSet_Holder_perms];
                  code:Code = HIGH;
                }

                table MapValueSet_Holder_codes {
                  key:string;
                  value:Code = LOW;
                }

                table MapValueSet_Holder_colors {
                  key:string;
                  value:Color;
                }

                table MapValueSet_Holder_perms {
                  key:string;
                  value:Perm;
                }

                root_type Holder;
                """, Files.readString(tempDir.resolve("enums.fbs")));
    }
}
