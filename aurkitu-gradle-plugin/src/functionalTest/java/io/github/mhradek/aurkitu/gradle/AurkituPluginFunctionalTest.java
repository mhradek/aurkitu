package io.github.mhradek.aurkitu.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The plugin in real Gradle builds (TestKit), on the Maven plugin's end-to-end test projects where they apply. */
class AurkituPluginFunctionalTest {

    private static final String VERSION = System.getProperty("aurkitu.version");
    private static final Path IT_DIR = Path.of(System.getProperty("aurkitu.itDir"));
    private static final String LOCAL_REPO = Path.of(System.getProperty("maven.repo.local")).toUri().toString();
    private static final String TASK = ":generateFlatBuffersSchema";

    @TempDir
    Path project;

    /** Same sources and options as the Maven kitchen-sink project: the schema must be the same, byte for byte. */
    @Test
    void kitchenSinkSchemaMatchesTheMavenPlugin() throws IOException {
        copyTree(IT_DIR.resolve("kitchen-sink/src"), project.resolve("src"));
        settings("kitchen-sink");
        build("""
                aurkitu {
                    schemaName = 'kitchen'
                    schemaNamespace = 'com.example.kitchensink'
                    specifiedPackages = ['com.example.kitchensink']
                    fileIdentifier = 'KTCH'
                    fileExtension = 'ktch'
                    includes = ['shared.fbs']
                    failOnValidationIssues = true
                    optionalScalars = true
                    snakeCaseAcronyms = true
                    typedMapKeys = true
                }
                """);

        BuildResult result = run("assemble");

        assertEquals(TaskOutcome.SUCCESS, result.task(TASK).getOutcome());
        assertEquals(normalise(Files.readString(IT_DIR.resolve("kitchen-sink/expected/kitchen.fbs"))),
                normalise(Files.readString(project.resolve("build/aurkitu/schemas/kitchen.fbs"))));
    }

    /** Annotated classes in another project of the build are found through the compile classpath. */
    @Test
    void modelsInADependencyAreScanned() throws IOException {
        Path models = IT_DIR.resolve("dependency-models/models/src");
        Path service = IT_DIR.resolve("dependency-models/service");
        copyTree(models, project.resolve("models/src"));
        copyTree(service.resolve("src"), project.resolve("service/src"));
        Files.writeString(project.resolve("settings.gradle"), """
                rootProject.name = 'dependency-models'
                include 'models', 'service'
                """);
        Files.writeString(project.resolve("models/build.gradle"), """
                plugins { id 'java-library' }
                %s
                dependencies { implementation 'io.github.mhradek:aurkitu-annotations:%s' }
                """.formatted(repositories(), VERSION));
        Files.writeString(project.resolve("service/build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.mhradek.aurkitu'
                }
                %s
                dependencies {
                    implementation project(':models')
                    implementation 'io.github.mhradek:aurkitu-annotations:%s'
                }
                // As in the Maven project's POM.
                aurkitu {
                    schemaName = 'service'
                    schemaNamespace = 'com.example.service'
                    specifiedPackages = ['com.example.models', 'com.example.service']
                    failOnValidationIssues = true
                }
                """.formatted(repositories(), VERSION));

        run(":service" + TASK);

        assertEquals(normalise(Files.readString(service.resolve("expected/service.fbs"))),
                normalise(Files.readString(project.resolve("service/build/aurkitu/schemas/service.fbs"))));
    }

    @Test
    void anUnchangedBuildIsUpToDateAndAChangedClassRunsAgain() throws IOException {
        simpleProject("");

        assertEquals(TaskOutcome.SUCCESS, run(TASK).task(TASK).getOutcome());
        assertEquals(TaskOutcome.UP_TO_DATE, run(TASK).task(TASK).getOutcome());

        // A private field is part of the schema, so it must count as a change.
        Path model = project.resolve("src/main/java/com/example/Person.java");
        Files.writeString(model, Files.readString(model).replace("private String name;", "private String name;\n    private int age;"));
        assertEquals(TaskOutcome.SUCCESS, run(TASK).task(TASK).getOutcome());
        assertTrue(Files.readString(project.resolve("build/aurkitu/schemas/people.fbs")).contains("age:int;"));
    }

    @Test
    void worksWithTheConfigurationCache() throws IOException {
        simpleProject("");

        BuildResult first = run("--configuration-cache", TASK);
        assertEquals(TaskOutcome.SUCCESS, first.task(TASK).getOutcome());
        BuildResult second = run("--configuration-cache", TASK);
        assertTrue(second.getOutput().contains("Reusing configuration cache"), second.getOutput());
    }

    @Test
    void validationIssuesFailTheBuildWhenAskedTo() throws IOException {
        copyTree(IT_DIR.resolve("fail-on-validation/src"), project.resolve("src"));
        settings("failing");
        build("""
                aurkitu {
                    schemaName = 'failing'
                    failOnValidationIssues = true
                }
                """);

        BuildResult result = runner(TASK).buildAndFail();

        assertEquals(TaskOutcome.FAILED, result.task(TASK).getOutcome());
        assertTrue(result.getOutput().contains("Schema validation failed with"), result.getOutput());
        assertTrue(Files.exists(project.resolve("build/aurkitu/schemas/failing.fbs")), "the schema is written either way");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void flatcRejectionFailsTheBuild() throws IOException {
        Path flatc = project.resolve("fake-flatc");
        Files.writeString(flatc, "#!/bin/sh\necho 'error: something is wrong'\nexit 1\n");
        assertTrue(flatc.toFile().setExecutable(true));
        simpleProject("flatcPath = '" + flatc + "'");

        BuildResult result = runner(TASK).buildAndFail();

        assertTrue(result.getOutput().contains("flatc validation failed for people.fbs"), result.getOutput());
        assertTrue(result.getOutput().contains("error: something is wrong"), result.getOutput());
    }

    /** The generated schema compiles with a real flatc, when FLATC_PATH points to one (as in CI). */
    @Test
    void realFlatcAcceptsTheSchema() throws IOException {
        String flatc = System.getenv("FLATC_PATH");
        if (flatc == null || flatc.isBlank()) {
            assertNotEquals("true", System.getenv("CI"), "FLATC_PATH must be set in CI");
            return;
        }
        simpleProject("flatcPath = '" + flatc.replace("\\", "\\\\") + "'");
        assertTrue(run(TASK).getOutput().contains("BUILD SUCCESSFUL"));
    }

    @Test
    void withoutAJavaPluginTheTaskExistsButHasNothingToScan() throws IOException {
        settings("bare");
        Files.writeString(project.resolve("build.gradle"), "plugins { id 'io.github.mhradek.aurkitu' }\n");

        BuildResult result = runner(TASK).buildAndFail();

        assertTrue(result.getOutput().contains("nothing to scan"), result.getOutput());
    }

    // --- helpers ---

    private void simpleProject(String extraConfiguration) throws IOException {
        settings("people");
        Path source = Files.createDirectories(project.resolve("src/main/java/com/example"));
        Files.writeString(source.resolve("Person.java"), """
                package com.example;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable(rootType = true)
                public class Person {
                    private String name;
                    private Mood mood;
                }
                """);
        Files.writeString(source.resolve("Mood.java"), """
                package com.example;

                import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

                @FlatBufferEnum
                public enum Mood { HAPPY, SAD }
                """);
        build("""
                aurkitu {
                    schemaName = 'people'
                    schemaNamespace = 'com.example.fb'
                    specifiedPackages = ['com.example']
                    %s
                }
                """.formatted(extraConfiguration));
    }

    private void settings(String name) throws IOException {
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = '" + name + "'\n");
    }

    private void build(String aurkitu) throws IOException {
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.mhradek.aurkitu'
                }
                %s
                dependencies {
                    implementation 'io.github.mhradek:aurkitu-annotations:%s'
                }
                %s
                """.formatted(repositories(), VERSION, aurkitu));
    }

    private static String repositories() {
        String mirror = System.getProperty("aurkitu.centralMirror");
        return """
                repositories {
                    maven { url = '%s' }
                    %s
                }
                """.formatted(LOCAL_REPO, mirror == null ? "mavenCentral()" : "maven { url = '" + mirror + "' }");
    }

    private BuildResult run(String... arguments) {
        return runner(arguments).build();
    }

    private GradleRunner runner(String... arguments) {
        List<String> args = new ArrayList<>(Arrays.asList(arguments));
        args.add("--stacktrace");
        return GradleRunner.create()
                .withProjectDir(project.toFile())
                .withPluginClasspath()
                .withArguments(args)
                .forwardOutput();
    }

    /** Same normalisation as the Maven ITs' common.groovy: the header carries the version. */
    private static String normalise(String schema) {
        return schema.replace("\r\n", "\n").replaceFirst("^// Aurkitu \\S+ ", "// Aurkitu @VERSION@ ");
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            paths.forEach(source -> {
                Path target = to.resolve(from.relativize(source).toString());
                try {
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
