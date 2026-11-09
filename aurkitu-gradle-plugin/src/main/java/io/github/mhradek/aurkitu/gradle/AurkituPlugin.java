package io.github.mhradek.aurkitu.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

/**
 * {@code io.github.mhradek.aurkitu}: adds the {@code aurkitu { }} extension and the
 * {@value #TASK_NAME} task. With a Java (or Kotlin JVM) plugin applied, the task scans the {@code main}
 * classes and their compile classpath, and {@code assemble} runs it.
 */
public class AurkituPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "aurkitu";
    public static final String TASK_NAME = "generateFlatBuffersSchema";

    @Override
    public void apply(Project project) {
        AurkituExtension extension = project.getExtensions().create(EXTENSION_NAME, AurkituExtension.class);
        extension.getSchemaName().convention(project.getName());
        extension.getSchemaNamespace().convention("generated.flatbuffers");
        extension.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("aurkitu/schemas"));
        extension.getValidate().convention(true);
        extension.getIgnoreStaticMembers().convention(true);
        extension.getMapCommonTypes().convention(true);
        extension.getCacheEnabled().convention(false);
        extension.getSnakeCaseFields().convention(true);
        extension.getSnakeCaseAcronyms().convention(true);
        extension.getTypedMapKeys().convention(false);
        extension.getGenerateVersion().convention(false);
        extension.getFailOnValidationIssues().convention(false);
        extension.getOptionalScalars().convention(false);

        TaskProvider<GenerateFlatBuffersSchema> task = project.getTasks().register(TASK_NAME, GenerateFlatBuffersSchema.class, t -> {
            t.setGroup("build");
            t.setDescription("Generates a FlatBuffers schema (.fbs) from the annotated classes.");
            t.getSchemaName().set(extension.getSchemaName());
            t.getSchemaNamespace().set(extension.getSchemaNamespace());
            t.getOutputDirectory().set(extension.getOutputDirectory());
            t.getSpecifiedPackages().set(extension.getSpecifiedPackages());
            t.getValidate().set(extension.getValidate());
            t.getIgnoreStaticMembers().set(extension.getIgnoreStaticMembers());
            t.getFileIdentifier().set(extension.getFileIdentifier());
            t.getIncludes().set(extension.getIncludes());
            t.getMapCommonTypes().set(extension.getMapCommonTypes());
            t.getFlatcPath().set(extension.getFlatcPath());
            t.getCacheEnabled().set(extension.getCacheEnabled());
            t.getSnakeCaseFields().set(extension.getSnakeCaseFields());
            t.getSnakeCaseAcronyms().set(extension.getSnakeCaseAcronyms());
            t.getTypedMapKeys().set(extension.getTypedMapKeys());
            t.getGenerateVersion().set(extension.getGenerateVersion());
            t.getFailOnValidationIssues().set(extension.getFailOnValidationIssues());
            t.getOptionalScalars().set(extension.getOptionalScalars());
            t.getFileExtension().set(extension.getFileExtension());
        });

        project.getPluginManager().withPlugin("java", java -> {
            SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            // The Maven plugin scans the compile classpath, which starts with the project's own classes.
            task.configure(t -> t.getClasspath().from(main.getOutput().getClassesDirs(), main.getCompileClasspath()));
            project.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME).configure(assemble -> assemble.dependsOn(task));
        });
    }
}
