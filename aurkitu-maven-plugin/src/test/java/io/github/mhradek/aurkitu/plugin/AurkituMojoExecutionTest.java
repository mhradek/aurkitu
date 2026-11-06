package io.github.mhradek.aurkitu.plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mojo execution paths: flatc success, rejection and interruption (using small fake
 * {@code flatc} scripts, so no binary is needed), and how failures are reported to Maven.
 */
class AurkituMojoExecutionTest {

    private static final String MODELS = "io.github.mhradek.aurkitu.plugin.core.testmodels";

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

        mojo(flatc.toString()).execute();

        String invocation = Files.readString(args).trim();
        assertTrue(invocation.startsWith("--binary --schema -o "), invocation);
        assertTrue(invocation.endsWith(tempDir.resolve("out/models.fbs").toString()), invocation);
        String scratch = invocation.split(" ")[3];
        assertFalse(Files.exists(Path.of(scratch)), "flatc output directory is deleted afterwards");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void flatcRejectionFailsWithItsOutput() throws Exception {
        Path flatc = script("flatc-reject", "echo 'error: something is wrong'\nexit 1\n");

        MojoExecutionException e = assertThrows(MojoExecutionException.class, () -> mojo(flatc.toString()).execute());
        assertTrue(e.getMessage().contains("flatc validation failed for models.fbs"), e.getMessage());
        assertTrue(e.getMessage().contains("error: something is wrong"), e.getMessage());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void interruptionWhileWaitingForFlatcIsReportedAndPreserved() throws Exception {
        // Signals that it started, closes its output (so reading finishes), then keeps running.
        Path started = tempDir.resolve("started");
        Path flatc = script("flatc-slow", "touch '" + started + "'\nexec >&- 2>&-\nsleep 5\n");
        AurkituMojo mojo = mojo(flatc.toString());

        Thread mojoThread = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                while (!Files.exists(started)) {
                    Thread.sleep(10);
                }
                mojoThread.interrupt();
            } catch (InterruptedException ignored) {
                // test is over
            }
        });
        interrupter.start();
        try {
            MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
            assertEquals("Interrupted while running flatc", e.getMessage());
            assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag is restored");
        } finally {
            interrupter.interrupt();
            interrupter.join();
        }
    }

    @Test
    void flatcIsSkippedWhenValidationIsOffOrNoPathIsSet() throws Exception {
        AurkituMojo noValidation = mojo("/nonexistent/flatc");
        set(noValidation, "validate", false);
        noValidation.execute(); // would fail if flatc were run

        mojo("   ").execute();
        assertTrue(Files.exists(tempDir.resolve("out/models.fbs")));
    }

    @Test
    void validationFailureIsABuildFailure() throws Exception {
        AurkituMojo mojo = mojo(null);
        set(mojo, "failOnValidationIssues", true);

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(e.getMessage().startsWith("Schema validation failed with"), e.getMessage());
    }

    @Test
    void outputDirectoryThatCannotBeCreatedFails() throws Exception {
        Path file = Files.writeString(tempDir.resolve("not-a-directory"), "x");
        AurkituMojo mojo = mojo(null);
        set(mojo, "outputDirectory", file.resolve("schemas").toFile());

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertTrue(e.getMessage().startsWith("Failed to create output directory"), e.getMessage());
    }

    @Test
    void unresolvableClasspathFails() throws Exception {
        AurkituMojo mojo = mojo(null);
        set(mojo, "project", new MavenProject() {
            @Override
            public List<String> getCompileClasspathElements() throws DependencyResolutionRequiredException {
                throw new DependencyResolutionRequiredException(null);
            }
        });

        MojoExecutionException e = assertThrows(MojoExecutionException.class, mojo::execute);
        assertEquals("Failed to resolve project compile classpath", e.getMessage());
    }

    private AurkituMojo mojo(String flatcPath) throws Exception {
        AurkituMojo mojo = new AurkituMojo();
        set(mojo, "schemaName", "models");
        set(mojo, "schemaNamespace", "test.models");
        set(mojo, "outputDirectory", tempDir.resolve("out").toFile());
        set(mojo, "specifiedPackages", List.of(MODELS));
        set(mojo, "validate", true);
        set(mojo, "ignoreStaticMembers", true);
        set(mojo, "mapCommonTypes", true);
        set(mojo, "snakeCaseFields", true);
        set(mojo, "flatcPath", flatcPath);
        return mojo;
    }

    private Path script(String name, String body) throws Exception {
        Path script = tempDir.resolve(name);
        Files.writeString(script, "#!/bin/sh\n" + body);
        assertTrue(script.toFile().setExecutable(true));
        return script;
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
