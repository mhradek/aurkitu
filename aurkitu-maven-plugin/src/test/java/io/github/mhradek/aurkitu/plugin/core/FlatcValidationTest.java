package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style test that runs flatc against generated schemas.
 * Skipped unless the flatc binary is available on the PATH or via the
 * system property {@code flatc.path} / environment variable {@code FLATC_PATH}.
 */
class FlatcValidationTest {

    @TempDir
    Path tempDir;

    static boolean flatcAvailable() {
        String path = System.getProperty("flatc.path", System.getenv("FLATC_PATH"));
        if (path != null && !path.isBlank() && new File(path).canExecute()) {
            return true;
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File candidate = new File(dir, "flatc");
                if (candidate.canExecute()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String findFlatc() {
        String path = System.getProperty("flatc.path", System.getenv("FLATC_PATH"));
        if (path != null && !path.isBlank() && new File(path).canExecute()) {
            return path;
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                File candidate = new File(dir, "flatc");
                if (candidate.canExecute()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return "flatc";
    }

    @Test
    @EnabledIf("flatcAvailable")
    void generatedSchemaIsAcceptedByFlatc() throws Exception {
        SchemaGenerator generator = new SchemaGenerator(
                null,
                List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"),
                true);
        File out = tempDir.toFile();
        generator.generate("models", "test.models", out);

        Path schema = out.toPath().resolve("models.fbs");
        assertTrue(Files.exists(schema));

        String flatc = findFlatc();
        ProcessBuilder pb = new ProcessBuilder(flatc, "--java", "-o", out.getAbsolutePath(), schema.toAbsolutePath().toString());
        pb.directory(out);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exit = process.waitFor();
        String output = new String(process.getInputStream().readAllBytes());

        assertEquals(0, exit, "flatc should accept the generated schema. Output:\n" + output);
    }
}
