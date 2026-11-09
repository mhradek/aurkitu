package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Types in several namespaces ({@code namespaces} package): enums and RPC services with their own
 * namespace, references qualified only across namespaces, union members resolved in the union's
 * namespace first, and a {@code namespace} statement only where the namespace changes.
 */
class NamespaceTest {

    @TempDir
    Path tempDir;

    @Test
    void typesAreScopedToTheirNamespacesAndReferencesQualifiedAcrossThem() throws Exception {
        new SchemaGenerator(List.of("io.github.mhradek.aurkitu.plugin.core.namespaces"), true)
                .generate("ns", "app", tempDir.toFile());
        String schema = Files.readString(tempDir.resolve("ns.fbs"));
        assertEquals(AurkituVersion.schemaHeader() + "\n" + """
                namespace app;

                namespace other;

                union Elsewhere {
                  Note,
                }

                enum Level : byte {
                  LOW,
                  HIGH,
                }

                namespace app;

                union Mixed {
                  Note,
                  other.Only,
                }

                table Note {
                  text:string;
                }

                namespace other;

                table Only {
                  value:int;
                }

                namespace app;

                table Root {
                  level:other.Level;
                  note:Note;
                  other_note:other.Note;
                  mixed:Mixed;
                }

                namespace other;

                table Note {
                  priority:int;
                }

                rpc_service Svc {
                  get(Only):Only;
                }

                namespace app;

                root_type Root;
                """, schema);
    }
}
