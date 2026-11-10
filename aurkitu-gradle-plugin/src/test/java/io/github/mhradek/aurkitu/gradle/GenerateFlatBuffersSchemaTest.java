package io.github.mhradek.aurkitu.gradle;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The task's action, run directly in a ProjectBuilder project on classes compiled by the test. */
class GenerateFlatBuffersSchemaTest {

    @TempDir
    Path dir;

    @Test
    void writesTheSchemaFromTheClasspath() throws Exception {
        GenerateFlatBuffersSchema task = task(compile("""
                package demo;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable(rootType = true)
                public class Ping {
                    private int count;
                }
                """));

        task.generate();

        String schema = Files.readString(dir.resolve("out/demo.fbs"));
        assertTrue(schema.contains("table Ping {\n  count:int;\n}"), schema);
        assertTrue(schema.contains("root_type Ping;"), schema);
    }

    @Test
    void validationFailureFailsTheTask() throws Exception {
        GenerateFlatBuffersSchema task = task(compile("""
                package demo;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable
                public class Lonely {
                    private int count;
                }
                """));
        task.getFailOnValidationIssues().set(true);

        GradleException e = assertThrows(GradleException.class, task::generate);
        assertTrue(e.getMessage().startsWith("Schema validation failed with"), e.getMessage());
    }

    @Test
    void anEmptyClasspathFailsInsteadOfScanningGradle() throws Exception {
        GenerateFlatBuffersSchema task = task(null);
        task.getClasspath().setFrom();

        GradleException e = assertThrows(GradleException.class, task::generate);
        assertTrue(e.getMessage().contains("nothing to scan"), e.getMessage());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void flatcRunsWhenValidatingAndItsRejectionFailsTheTask() throws Exception {
        Path flatc = dir.resolve("flatc");
        Files.writeString(flatc, "#!/bin/sh\necho 'error: rejected'\nexit 1\n");
        assertTrue(flatc.toFile().setExecutable(true));
        Path classes = compile("""
                package demo;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable(rootType = true)
                public class Ping {
                    private int count;
                }
                """);

        GenerateFlatBuffersSchema rejected = task(classes);
        rejected.getFlatcPath().set(flatc.toString());
        GradleException e = assertThrows(GradleException.class, rejected::generate);
        assertTrue(e.getMessage().contains("error: rejected"), e.getMessage());

        GenerateFlatBuffersSchema notValidating = task(classes);
        notValidating.getFlatcPath().set(flatc.toString());
        notValidating.getValidate().set(false);
        assertDoesNotThrow(notValidating::generate, "flatc only runs with validate");

        GenerateFlatBuffersSchema blank = task(classes);
        blank.getFlatcPath().set("  ");
        assertDoesNotThrow(blank::generate, "a blank path is no flatc");
    }

    /** The task, as Gradle would run it: Gradle creates the output directory before running the action. */
    private GenerateFlatBuffersSchema task(Path classes) throws java.io.IOException {
        Files.createDirectories(dir.resolve("out"));
        Project project = ProjectBuilder.builder().withProjectDir(dir.resolve("project").toFile()).build();
        project.getPluginManager().apply(AurkituPlugin.class);
        GenerateFlatBuffersSchema task = (GenerateFlatBuffersSchema) project.getTasks().getByName(AurkituPlugin.TASK_NAME);
        task.getSchemaName().set("demo");
        task.getSpecifiedPackages().add("demo");
        task.getOutputDirectory().set(dir.resolve("out").toFile());
        if (classes != null) {
            task.getClasspath().from(classes);
        }
        return task;
    }

    private Path compile(String source) throws Exception {
        Path src = Files.createDirectories(dir.resolve("src/demo"));
        String name = source.replaceAll("(?s).*public class (\\w+).*", "$1");
        Path file = Files.writeString(src.resolve(name + ".java"), source);
        Path out = Files.createDirectories(dir.resolve("classes-" + name));
        String annotations = new File(FlatBufferTable.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, javac.run(null, null, null, "-d", out.toString(), "-cp", annotations, file.toString()), "compiles");
        return out;
    }
}
