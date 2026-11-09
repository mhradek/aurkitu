package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Clean-up of the scratch directory flatc writes into: best effort, never failing the build. */
class FlatcCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    void deletesTheDirectoryAndEverythingInIt() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("scratch/nested"));
        Files.writeString(dir.resolve("schema.bfbs"), "x");
        Files.writeString(tempDir.resolve("scratch/top.bfbs"), "y");

        Flatc.deleteQuietly(tempDir.resolve("scratch"));

        assertFalse(Files.exists(tempDir.resolve("scratch")));
    }

    @Test
    void aMissingOrNullDirectoryIsNotAnError() {
        assertDoesNotThrow(() -> Flatc.deleteQuietly(tempDir.resolve("missing")));
        assertDoesNotThrow(() -> Flatc.deleteQuietly(null));
    }

    @Test
    void whatCannotBeDeletedIsLeftWithoutFailing() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("busy"));
        Files.writeString(dir.resolve("file"), "x");

        Flatc.deleteOrLog(dir); // not empty: Files.delete throws, which is only logged

        assertTrue(Files.exists(dir.resolve("file")));
    }
}
