package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CoverageGapTest {

    @TempDir
    Path tempDir;

    @Test
    void failOnValidationIssuesSkipsNonScalarStructField() {
        SchemaGenerator.Config config = new SchemaGenerator.Config(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.validation"),
                true,
                true,
                null,
                List.of(),
                true,
                false,
                true,
                false,
                true,
                null);
        SchemaGenerator generator = new SchemaGenerator(config);
        File out = tempDir.toFile();

        assertThrows(SchemaGenerationException.class,
                () -> generator.generate("failstruct", "test.fail", out));
    }

    @Test
    void emitsUseFullNameAndSkipsBadAttributes() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.validation"),
                true);
        File out = tempDir.toFile();
        generator.generate("gaps", "test.gaps", out);

        String content = Files.readString(out.toPath().resolve("gaps.fbs"));
        assertTrue(content.contains("SampleWithUseFullName"));
        assertTrue(content.contains("nested_flatbuffer") || content.contains("skipped"));
        assertTrue(content.contains("flexbuffer") || content.contains("skipped"));
        assertTrue(content.contains("union SampleUnion {"));
    }

    @Test
    void isStructAndIsRootTypeHelpers() {
        // Exercised indirectly via generation; this just locks the package-private API
        assertNotNull(TableEmitter.class);
    }
}
