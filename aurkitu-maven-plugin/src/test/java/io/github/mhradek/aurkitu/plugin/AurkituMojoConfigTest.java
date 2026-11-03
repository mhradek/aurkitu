package io.github.mhradek.aurkitu.plugin;

import io.github.mhradek.aurkitu.plugin.core.SchemaGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight integration-style test that exercises SchemaGenerator.Config
 * construction the same way AurkituMojo does, and verifies output is written.
 */
class AurkituMojoConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void configProducesSchemaFile() throws Exception {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true,
                true,
                "AB12",
                List.of(),
                true,
                false,
                true,
                true,
                false);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();
        generator.generate("mojo-check", "test.mojo", out);

        Path schema = out.toPath().resolve("mojo-check.fbs");
        assertTrue(Files.exists(schema));
        String content = Files.readString(schema);
        assertTrue(content.contains("file_identifier \"AB12\";"));
        assertTrue(content.contains("// @version:"));
        assertTrue(content.contains("table SampleRootTable"));
    }
}
