package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Class files only hold the annotation elements that were written, so the plugin supplies the defaults
 * itself. Models that spell out every default must therefore generate exactly the same schema as models
 * that leave them out ({@code defaults.explicit} vs {@code defaults.implicit}).
 */
class AnnotationDefaultsTest {

    @TempDir
    Path tempDir;

    @Test
    void explicitDefaultsGenerateTheSameSchemaAsOmittedOnes() throws Exception {
        String implicit = generate("implicit");
        String explicit = generate("explicit");
        assertEquals(implicit, explicit);
        assertTrue(implicit.contains("""
                struct Shape {
                  x:float;
                }

                table Thing {
                  count:int;
                  boxed:int;
                  kind:Kind;
                  shape:Shape;
                }
                """), implicit);
        assertTrue(implicit.contains("enum Kind : byte {\n  A,\n  B,\n}\n"), implicit);
        assertTrue(implicit.contains("rpc_service Svc {\n  get(Thing):Thing;\n}\n"), implicit);
    }

    private String generate(String pkg) throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve(pkg));
        new SchemaGenerator(null, List.of("io.github.mhradek.aurkitu.plugin.core.defaults." + pkg), true)
                .generate("defaults", "d", dir.toFile());
        return Files.readString(dir.resolve("defaults.fbs"));
    }
}
