package io.github.mhradek.aurkitu.plugin.core;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Checks a written schema with {@code flatc}, for the build plugins' {@code flatcPath} option. */
@Slf4j
public final class Flatc {

    private Flatc() {}

    /**
     * Validates the schema by compiling it to a binary schema ({@code flatc --binary --schema}), which parses
     * and checks it fully without targeting a language. Output is discarded.
     *
     * @throws SchemaGenerationException if flatc rejects the schema (the message holds flatc's output), cannot
     *                                   be run, or the thread is interrupted while waiting for it (the interrupt
     *                                   flag is restored)
     */
    public static void validate(String flatcPath, File schemaFile) {
        Path scratch = null;
        try {
            scratch = Files.createTempDirectory("aurkitu-flatc");
            ProcessBuilder pb = new ProcessBuilder(flatcPath, "--binary", "--schema",
                    "-o", scratch.toString(), schemaFile.getAbsolutePath());
            pb.directory(schemaFile.getParentFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            // Drain output before waiting so a full pipe cannot block flatc.
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new SchemaGenerationException("flatc validation failed for " + schemaFile.getName() + ":\n" + output);
            }
            log.info("flatc validation succeeded for {}", schemaFile.getName());
        } catch (IOException e) {
            throw new SchemaGenerationException("Failed to run flatc", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SchemaGenerationException("Interrupted while running flatc", e);
        } finally {
            deleteQuietly(scratch);
        }
    }

    /** Deletes {@code dir} and its contents, best effort: a leftover temp file is harmless. */
    static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        // Deepest first, so each directory is empty when it is deleted.
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(Flatc::deleteOrLog);
        } catch (IOException e) {
            log.debug("Could not clean up {}", dir, e);
        }
    }

    /** Deletes one file or empty directory; logs instead of failing when it cannot. */
    static void deleteOrLog(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            log.debug("Could not delete {}", path, e);
        }
    }
}
