package io.github.mhradek.aurkitu.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The plugin's wiring, in a project built in this JVM (ProjectBuilder): defaults, tasks and their conventions. */
class AurkituPluginTest {

    @TempDir
    Path dir;

    private Project project(boolean java) {
        Project project = ProjectBuilder.builder().withName("models").withProjectDir(dir.toFile()).build();
        if (java) {
            project.getPluginManager().apply("java");
        }
        project.getPluginManager().apply(AurkituPlugin.class);
        return project;
    }

    @Test
    void defaultsMatchTheMavenPluginExceptWhereGradleDiffers() {
        AurkituExtension aurkitu = project(true).getExtensions().getByType(AurkituExtension.class);

        assertEquals("models", aurkitu.getSchemaName().get(), "the project's name");
        assertEquals("generated.flatbuffers", aurkitu.getSchemaNamespace().get());
        assertEquals(new File(dir.toFile(), "build/aurkitu/schemas"), aurkitu.getOutputDirectory().get().getAsFile());
        assertEquals(List.of(), aurkitu.getSpecifiedPackages().get());
        assertEquals(List.of(), aurkitu.getIncludes().get());
        assertTrue(aurkitu.getValidate().get());
        assertTrue(aurkitu.getIgnoreStaticMembers().get());
        assertTrue(aurkitu.getMapCommonTypes().get());
        assertTrue(aurkitu.getSnakeCaseFields().get());
        assertTrue(aurkitu.getSnakeCaseAcronyms().get());
        assertFalse(aurkitu.getCacheEnabled().get());
        assertFalse(aurkitu.getTypedMapKeys().get());
        assertFalse(aurkitu.getGenerateVersion().get());
        assertFalse(aurkitu.getFailOnValidationIssues().get());
        assertFalse(aurkitu.getOptionalScalars().get());
        assertNull(aurkitu.getFileIdentifier().getOrNull());
        assertNull(aurkitu.getFlatcPath().getOrNull());
        assertNull(aurkitu.getFileExtension().getOrNull());
    }

    @Test
    void theTaskTakesItsOptionsFromTheExtensionUnlessItSetsItsOwn() {
        Project project = project(true);
        AurkituExtension aurkitu = project.getExtensions().getByType(AurkituExtension.class);
        aurkitu.getSchemaNamespace().set("com.example.fb");
        aurkitu.getFailOnValidationIssues().set(true);
        GenerateFlatBuffersSchema task = schemaTask(project, AurkituPlugin.TASK_NAME);

        assertEquals("com.example.fb", task.getSchemaNamespace().get());
        assertTrue(task.getFailOnValidationIssues().get());
        assertEquals("build", task.getGroup());
        assertNotNull(task.getDescription());

        task.getSchemaNamespace().set("own.namespace");
        assertEquals("own.namespace", task.getSchemaNamespace().get(), "the task's own setting wins");
    }

    @Test
    void assembleRunsEverySchemaTaskAndEachWritesToItsOwnDirectory() {
        Project project = project(true);
        project.getTasks().register("generateEventsSchema", GenerateFlatBuffersSchema.class,
                t -> t.setDescription("Events"));
        GenerateFlatBuffersSchema main = schemaTask(project, AurkituPlugin.TASK_NAME);
        GenerateFlatBuffersSchema events = schemaTask(project, "generateEventsSchema");

        assertEquals(new File(dir.toFile(), "build/aurkitu/schemas"), main.getOutputDirectory().get().getAsFile());
        assertEquals(new File(dir.toFile(), "build/aurkitu/generateEventsSchema"), events.getOutputDirectory().get().getAsFile());
        assertEquals("Events", events.getDescription(), "a description the build sets is kept");
        assertFalse(events.getClasspath().isEmpty(), "scans the main classes too");

        Task assemble = project.getTasks().getByName("assemble");
        Set<? extends Task> dependencies = assemble.getTaskDependencies().getDependencies(assemble);
        assertTrue(dependencies.contains(main), dependencies.toString());
        assertTrue(dependencies.contains(events), dependencies.toString());
    }

    @Test
    void withoutAJavaPluginThereIsNothingToScan() {
        GenerateFlatBuffersSchema task = schemaTask(project(false), AurkituPlugin.TASK_NAME);
        assertTrue(task.getClasspath().isEmpty());
    }

    @Test
    void propertiesAreReadAsMavenReadsItsUserProperties() {
        assertTrue(AurkituPlugin.parseBoolean(" TRUE "));
        assertFalse(AurkituPlugin.parseBoolean("yes"));
        assertEquals(List.of("com.a", "com.b"), AurkituPlugin.splitList(" com.a, ,com.b ,"));
        assertEquals(List.of(), AurkituPlugin.splitList(" "));
    }

    private static GenerateFlatBuffersSchema schemaTask(Project project, String name) {
        return (GenerateFlatBuffersSchema) project.getTasks().getByName(name);
    }
}
