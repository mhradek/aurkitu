package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Running flatc on a schema, with small fake {@code flatc} scripts so no binary is needed. */
class FlatcTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void flatcIsRunOnTheSchemaAsABinarySchemaCompile() throws Exception {
        Path args = tempDir.resolve("args.txt");
        Path flatc = script("flatc-ok", "echo \"$@\" > '" + args + "'\nexit 0\n");
        Path schema = schema();

        Flatc.validate(flatc.toString(), schema.toFile());

        String invocation = Files.readString(args).trim();
        assertTrue(invocation.startsWith("--binary --schema -o "), invocation);
        assertTrue(invocation.endsWith(schema.toString()), invocation);
        String scratch = invocation.split(" ")[3];
        assertFalse(Files.exists(Path.of(scratch)), "flatc output directory is deleted afterwards");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void rejectionFailsWithFlatcsOutput() throws Exception {
        Path flatc = script("flatc-reject", "echo 'error: something is wrong'\nexit 1\n");

        SchemaGenerationException e = assertThrows(SchemaGenerationException.class,
                () -> Flatc.validate(flatc.toString(), schema().toFile()));
        assertTrue(e.getMessage().startsWith("flatc validation failed for models.fbs:\n"), e.getMessage());
        assertTrue(e.getMessage().contains("error: something is wrong"), e.getMessage());
    }

    @Test
    void aFlatcThatCannotBeRunFails() throws Exception {
        Path schema = schema();
        SchemaGenerationException e = assertThrows(SchemaGenerationException.class,
                () -> Flatc.validate(tempDir.resolve("no-such-flatc").toString(), schema.toFile()));
        assertEquals("Failed to run flatc", e.getMessage());
        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void interruptionIsReportedAndTheFlagRestored() throws Exception {
        // Signals that it started, closes its output (so reading finishes), then keeps running.
        Path started = tempDir.resolve("started");
        Path flatc = script("flatc-slow", "touch '" + started + "'\nexec >&- 2>&-\nsleep 5\n");
        Path schema = schema();

        Thread caller = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                long deadline = System.nanoTime() + 30_000_000_000L;
                while (!Files.exists(started) && System.nanoTime() < deadline) {
                    Thread.sleep(10);
                }
                caller.interrupt();
            } catch (InterruptedException ignored) {
                // test is over
            }
        });
        interrupter.start();
        SchemaGenerationException e;
        boolean flagRestored;
        try {
            e = assertThrows(SchemaGenerationException.class, () -> Flatc.validate(flatc.toString(), schema.toFile()));
        } finally {
            // Read and clear the flag before join(), which would otherwise throw while it is set.
            flagRestored = Thread.interrupted();
            interrupter.interrupt();
            interrupter.join();
        }
        assertEquals("Interrupted while running flatc", e.getMessage());
        assertInstanceOf(InterruptedException.class, e.getCause());
        assertTrue(flagRestored, "interrupt flag is restored");
    }

    private Path schema() throws IOException {
        Path dir = Files.createDirectories(tempDir.resolve("out"));
        return Files.writeString(dir.resolve("models.fbs"), "table A {}\nroot_type A;\n");
    }

    private Path script(String name, String body) throws Exception {
        Path script = tempDir.resolve(name);
        Files.writeString(script, "#!/bin/sh\n" + body);
        assertTrue(script.toFile().setExecutable(true));
        return script;
    }
}
