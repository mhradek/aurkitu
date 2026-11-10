package io.github.mhradek.aurkitu.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** The plugin in real Gradle builds (TestKit), on the Maven plugin's end-to-end test projects where they apply. */
class AurkituPluginFunctionalTest {

    private static final String VERSION = System.getProperty("aurkitu.version");
    private static final Path IT_DIR = Path.of(System.getProperty("aurkitu.itDir"));
    private static final String LOCAL_REPO = Path.of(System.getProperty("maven.repo.local")).toUri().toString();
    private static final String TASK = ":generateFlatBuffersSchema";
    /** The Kotlin Gradle plugin version the Kotlin project test uses. */
    private static final String KOTLIN_VERSION = "2.4.21";

    @TempDir
    Path project;

    /**
     * Every single-project Maven end-to-end project in {@code aurkitu-maven-plugin/src/it}, built with Gradle using
     * the plugin configuration of its POM: it must succeed or fail as the Maven build does, and write the same
     * schema byte for byte. A new Maven project there is covered here automatically.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("mavenProjects")
    void mavenEndToEndProjectBehavesTheSame(String name) throws Exception {
        Path maven = IT_DIR.resolve(name);
        MavenProject it = MavenProject.read(maven);
        assumeTrue(it.runnable(), name + " needs " + it.requiredEnv() + ", which is not set");
        copyTree(maven.resolve("src"), project.resolve("src"));
        settings(name);
        build("aurkitu {\n" + it.groovyConfiguration() + "\n}\n");

        BuildResult result = it.expectFailure() ? runner(TASK).buildAndFail() : run(TASK);

        if (it.expectFailure()) {
            assertEquals(TaskOutcome.FAILED, result.task(TASK).getOutcome());
            assertTrue(result.getOutput().contains("Schema validation failed with"), result.getOutput());
        } else {
            assertEquals(TaskOutcome.SUCCESS, result.task(TASK).getOutcome());
        }
        Path expected = maven.resolve("expected/" + it.schemaName() + ".fbs");
        if (Files.exists(expected)) {
            assertEquals(normalise(Files.readString(expected)),
                    normalise(Files.readString(project.resolve("build/aurkitu/schemas/" + it.schemaName() + ".fbs"))));
        }
    }

    static Stream<String> mavenProjects() throws IOException {
        try (Stream<Path> dirs = Files.list(IT_DIR)) {
            List<String> names = dirs.filter(d -> Files.isDirectory(d.resolve("src")) && Files.exists(d.resolve("pom.xml")))
                    .map(d -> d.getFileName().toString()).sorted().toList();
            assertTrue(names.contains("kitchen-sink"), "found the Maven projects in " + IT_DIR + ": " + names);
            return names.stream();
        }
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

    /**
     * Options from Gradle properties, named as the Maven plugin's user properties ({@code -Daurkitu.validate=false}
     * there, {@code -Paurkitu.validate=false} here), on the command line or in gradle.properties. As in Maven, a
     * value set in the build script wins.
     */
    @Test
    void gradlePropertiesSetOptionsAndTheBuildScriptWins() throws IOException {
        peopleSources();
        Files.writeString(project.resolve("src/main/java/com/example/Badge.java"), """
                package com.example;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable
                public class Badge {
                    private String displayName;
                }
                """);
        Files.writeString(project.resolve("gradle.properties"), "aurkitu.snakeCaseFields=false\n");
        build("""
                aurkitu {
                    schemaNamespace = 'from.script'
                }
                """);

        run(TASK, "-Paurkitu.schemaName=fromProperty", "-Paurkitu.schemaNamespace=from.property",
                "-Paurkitu.specifiedPackages=com.example, com.nothing", "-Paurkitu.outputDir=schemas",
                "-Paurkitu.generateVersion=true");

        String schema = Files.readString(project.resolve("schemas/fromProperty.fbs"));
        assertTrue(schema.contains("namespace from.script;"), "the build script wins over -P:\n" + schema);
        assertTrue(schema.contains("// @version:"), "generateVersion from -P:\n" + schema);
        assertTrue(schema.contains("table Person {"), "specifiedPackages from -P:\n" + schema);
        assertTrue(schema.contains("displayName:string;"), "snakeCaseFields=false from gradle.properties:\n" + schema);
    }

    /**
     * More than one schema from one project: every GenerateFlatBuffersSchema task takes its options from
     * aurkitu { } unless it sets its own, scans the main classes, writes to its own directory and runs with assemble.
     */
    @Test
    void aProjectCanWriteSeveralSchemas() throws IOException {
        simpleProject("");
        Path eventSources = Files.createDirectories(project.resolve("src/main/java/com/example/events"));
        Files.writeString(eventSources.resolve("Clicked.java"), """
                package com.example.events;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable(rootType = true)
                public class Clicked {
                    private long atMillis;
                }
                """);
        Files.writeString(project.resolve("build.gradle"), Files.readString(project.resolve("build.gradle")) + """
                tasks.register('generateEventsSchema', io.github.mhradek.aurkitu.gradle.GenerateFlatBuffersSchema) {
                    schemaName = 'events'
                    specifiedPackages = ['com.example.events']
                }
                """);

        BuildResult result = run("assemble");

        assertEquals(TaskOutcome.SUCCESS, result.task(TASK).getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateEventsSchema").getOutcome());
        String events = Files.readString(project.resolve("build/aurkitu/generateEventsSchema/events.fbs"));
        assertTrue(events.contains("namespace com.example.fb;"), "namespace from aurkitu { }:\n" + events);
        assertTrue(events.contains("table Clicked {\n  at_millis:long;\n}"), events);
        assertFalse(events.contains("table Person"), "only its own packages:\n" + events);
        assertTrue(Files.readString(project.resolve("build/aurkitu/schemas/people.fbs")).contains("table Person"));
    }

    /**
     * The task is cacheable: after clean, and in a copy of the project at another path (a CI checkout, a colleague's
     * machine), the schema comes from the build cache instead of being generated again.
     */
    @Test
    void theSchemaComesFromTheBuildCacheAfterCleanAndInAnotherCheckout(@TempDir Path elsewhere) throws IOException {
        Path cache = elsewhere.resolve("build-cache");
        Path first = project.resolve("first");
        Files.createDirectories(first);
        withProject(first, () -> {
            simpleProject("");
            useBuildCache(cache);
        });

        assertEquals(TaskOutcome.SUCCESS, runIn(first, "--build-cache", TASK).task(TASK).getOutcome());
        runIn(first, "clean");
        assertEquals(TaskOutcome.FROM_CACHE, runIn(first, "--build-cache", TASK).task(TASK).getOutcome());

        Path second = elsewhere.resolve("second-checkout");
        copyTree(first.resolve("src"), second.resolve("src"));
        Files.copy(first.resolve("settings.gradle"), second.resolve("settings.gradle"));
        Files.copy(first.resolve("build.gradle"), second.resolve("build.gradle"));
        BuildResult relocated = runIn(second, "--build-cache", TASK);
        assertEquals(TaskOutcome.FROM_CACHE, relocated.task(TASK).getOutcome(), "relocatable: the path does not matter");
        assertEquals(Files.readString(first.resolve("build/aurkitu/schemas/people.fbs")),
                Files.readString(second.resolve("build/aurkitu/schemas/people.fbs")));
    }

    /** A Kotlin project: its classes (data classes, nullable types) are scanned like Java ones. */
    @Test
    void kotlinJvmProjectsWork() throws IOException {
        String mirror = System.getProperty("aurkitu.centralMirror");
        Files.writeString(project.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        %s
                        gradlePluginPortal()
                    }
                }
                rootProject.name = 'kotlin-models'
                """.formatted(mirror == null ? "" : "maven { url = '" + mirror + "' }"));
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '%s'
                    id 'io.github.mhradek.aurkitu'
                }
                %s
                dependencies {
                    implementation 'io.github.mhradek:aurkitu-annotations:%s'
                }
                aurkitu {
                    schemaNamespace = 'com.example.fb'
                    specifiedPackages = ['com.example']
                }
                """.formatted(KOTLIN_VERSION, repositories(), VERSION));
        Path source = Files.createDirectories(project.resolve("src/main/kotlin/com/example"));
        Files.writeString(source.resolve("Order.kt"), """
                package com.example

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable

                @FlatBufferTable(rootType = true)
                data class Order(val orderId: Long, val note: String?)
                """);

        BuildResult result = run("assemble");

        assertEquals(TaskOutcome.SUCCESS, result.task(TASK).getOutcome());
        String schema = Files.readString(project.resolve("build/aurkitu/schemas/kotlin-models.fbs"));
        assertTrue(schema.contains("table Order {\n  order_id:long;\n  note:string;\n}"), schema);
        assertTrue(schema.contains("root_type Order;"), schema);
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
        peopleSources();
        build("""
                aurkitu {
                    schemaName = 'people'
                    schemaNamespace = 'com.example.fb'
                    specifiedPackages = ['com.example']
                    %s
                }
                """.formatted(extraConfiguration));
    }

    /** The people project's settings and model classes, without a build script. */
    private void peopleSources() throws IOException {
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
    }

    private void useBuildCache(Path cache) throws IOException {
        Files.writeString(project.resolve("settings.gradle"), Files.readString(project.resolve("settings.gradle")) + """
                buildCache {
                    local {
                        directory = new File('%s')
                    }
                }
                """.formatted(cache.toString().replace("\\", "/")));
    }

    /** Runs {@code setup} with {@link #project} pointing at {@code dir}, so the helpers write there. */
    private void withProject(Path dir, IoAction setup) throws IOException {
        Path original = project;
        project = dir;
        try {
            setup.run();
        } finally {
            project = original;
        }
    }

    private BuildResult runIn(Path dir, String... arguments) {
        List<String> args = new ArrayList<>(Arrays.asList(arguments));
        args.add("--stacktrace");
        return GradleRunner.create().withProjectDir(dir.toFile()).withPluginClasspath().withArguments(args)
                .forwardOutput().build();
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
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
