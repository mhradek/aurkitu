package io.github.mhradek.aurkitu.plugin;

import io.github.mhradek.aurkitu.plugin.core.FakeFlatc;
import io.github.mhradek.aurkitu.plugin.core.SchemaGenerationException;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    void flatcIsRunOnTheSchemaAsABinarySchemaCompile() throws Exception {
        Path args = tempDir.resolve("args.txt");
        Path flatc = FakeFlatc.recordingArguments(tempDir, args);

        mojo(flatc.toString()).execute();

        String invocation = Files.readString(args).trim();
        assertTrue(invocation.startsWith("--binary --schema -o "), invocation);
        assertTrue(invocation.endsWith(tempDir.resolve("out/models.fbs").toString()), invocation);
        String scratch = invocation.split(" ")[3];
        assertFalse(Files.exists(Path.of(scratch)), "flatc output directory is deleted afterwards");
    }

    @Test
    void flatcRejectionFailsWithItsOutput() throws Exception {
        Path flatc = FakeFlatc.rejecting(tempDir, "error: something is wrong");

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
                // Gives up (and the test fails) rather than hanging if the script never starts.
                long deadline = System.nanoTime() + 30_000_000_000L;
                while (!Files.exists(started) && System.nanoTime() < deadline) {
                    Thread.sleep(10);
                }
                mojoThread.interrupt();
            } catch (InterruptedException ignored) {
                // test is over
            }
        });
        interrupter.start();
        MojoExecutionException e;
        boolean flagRestored;
        try {
            e = assertThrows(MojoExecutionException.class, mojo::execute);
        } finally {
            // Read and clear the flag before join(), which would otherwise throw while it is set.
            flagRestored = Thread.interrupted();
            interrupter.interrupt();
            interrupter.join();
        }
        assertEquals("Interrupted while running flatc", e.getMessage());
        assertTrue(flagRestored, "interrupt flag is restored");
    }

    @Test
    void flatcIsSkippedWhenValidationIsOffOrNoPathIsSet() throws Exception {
        Path schema = tempDir.resolve("out/models.fbs");
        AurkituMojo noValidation = mojo("/nonexistent/flatc");
        set(noValidation, "validate", false);
        noValidation.execute(); // would fail if flatc were run
        assertTrue(Files.exists(schema));

        Files.delete(schema);
        mojo("   ").execute(); // a blank path would fail too
        assertTrue(Files.exists(schema));
    }

    /**
     * validate x failOnValidationIssues x cacheEnabled x flatcPath (unset, blank, set), on models with issues:
     * the build fails exactly when validate and failOnValidationIssues are both on (before flatc), and flatc
     * runs exactly when validate is on, a path is set and the build has not failed; cached or not.
     */
    @ParameterizedTest
    @CsvSource({
            "false,false,false,none", "false,false,false,blank", "false,false,false,script",
            "false,false,true,none", "false,false,true,blank", "false,false,true,script",
            "false,true,false,none", "false,true,false,blank", "false,true,false,script",
            "false,true,true,none", "false,true,true,blank", "false,true,true,script",
            "true,false,false,none", "true,false,false,blank", "true,false,false,script",
            "true,false,true,none", "true,false,true,blank", "true,false,true,script",
            "true,true,false,none", "true,true,false,blank", "true,true,false,script",
            "true,true,true,none", "true,true,true,blank", "true,true,true,script"})
    void validationFailureAndFlatcInteract(boolean validate, boolean failOnIssues, boolean cache, String flatc) throws Exception {
        Path ran = tempDir.resolve("flatc-ran");
        String path = switch (flatc) {
            case "none" -> null;
            case "blank" -> " ";
            default -> FakeFlatc.touching(tempDir, ran).toString();
        };
        for (int run = 1; run <= 2; run++) { // the second run finds an unchanged schema
            Files.deleteIfExists(ran);
            AurkituMojo mojo = mojo(path);
            set(mojo, "validate", validate);
            set(mojo, "failOnValidationIssues", failOnIssues);
            set(mojo, "cacheEnabled", cache);
            String label = "validate=" + validate + " failOn=" + failOnIssues + " cache=" + cache + " flatc=" + flatc + " run=" + run;
            if (validate && failOnIssues) {
                assertThrows(MojoFailureException.class, mojo::execute, label);
            } else {
                mojo.execute();
            }
            assertTrue(Files.exists(tempDir.resolve("out/models.fbs")), label);
            assertEquals(validate && !failOnIssues && "script".equals(flatc), Files.exists(ran), label);
        }
    }

    @Test
    void validationFailureIsABuildFailure() throws Exception {
        AurkituMojo mojo = mojo(null);
        set(mojo, "failOnValidationIssues", true);

        MojoFailureException e = assertThrows(MojoFailureException.class, mojo::execute);
        assertEquals("Schema validation failed with 2 issue(s). See schema comments.", e.getMessage());
        assertInstanceOf(SchemaGenerationException.class, e.getCause());
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
