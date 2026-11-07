package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs flatc on generated schemas. Uses {@code flatc.path}, {@code FLATC_PATH} or {@code flatc} on the PATH.
 * Skipped locally when there is no flatc; on CI ({@code CI=true}) a missing flatc fails the test, so it
 * cannot silently stop running.
 */
class FlatcValidationTest {

    @TempDir
    Path tempDir;

    static Optional<String> findFlatc() {
        String path = System.getProperty("flatc.path", System.getenv("FLATC_PATH"));
        if (path != null && !path.isBlank() && new File(path).canExecute()) {
            return Optional.of(path);
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File candidate = new File(dir, "flatc");
                if (candidate.canExecute()) {
                    return Optional.of(candidate.getAbsolutePath());
                }
            }
        }
        return Optional.empty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"testmodels", "warnings"})
    void generatedSchemaIsAcceptedByFlatc(String pkg) throws Exception {
        Optional<String> flatc = findFlatc();
        if (flatc.isEmpty()) {
            assertNotEquals("true", System.getenv("CI"), "flatc must be available on CI");
            Assumptions.abort("flatc not found");
        }
        new SchemaGenerator(null, List.of("io.github.mhradek.aurkitu.plugin.core." + pkg), true)
                .generate(pkg, "test." + pkg, tempDir.toFile());
        Path schema = tempDir.resolve(pkg + ".fbs");

        Process process = new ProcessBuilder(flatc.get(), "--java", "-o", tempDir.resolve("java").toString(), schema.toString())
                .redirectErrorStream(true).start();
        // Read before waiting, so a full pipe cannot block flatc.
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();

        assertEquals(0, exit, "flatc should accept the generated schema. Output:\n" + output + "\nSchema:\n" + Files.readString(schema));
        try (var generated = Files.walk(tempDir.resolve("java"))) {
            assertTrue(generated.anyMatch(p -> p.toString().endsWith(".java")), "flatc generated no code");
        }
    }
}
