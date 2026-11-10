package io.github.mhradek.aurkitu.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code io.github.mhradek.aurkitu}: adds the {@code aurkitu { }} extension and the {@value #TASK_NAME} task.
 *
 * <ul>
 *   <li>Every option can also be set with a Gradle property of the same name as the Maven plugin's user property
 *       ({@code -Paurkitu.validate=false}, or {@code aurkitu.validate=false} in {@code gradle.properties}). As in
 *       Maven, a value set in the build script wins over the property.</li>
 *   <li>Any task of type {@link GenerateFlatBuffersSchema} is wired the same way, so a project can write several
 *       schemas: each task takes its options from {@code aurkitu { }} unless it sets its own, scans the {@code main}
 *       classes, writes to its own directory and is run by {@code assemble}.</li>
 * </ul>
 */
public class AurkituPlugin implements Plugin<Project> {

    /** The extension's name: {@code aurkitu { }}. */
    public static final String EXTENSION_NAME = "aurkitu";
    /** The task the plugin registers. */
    public static final String TASK_NAME = "generateFlatBuffersSchema";
    /** Prefix of the Gradle properties that set options, as the Maven plugin's user properties. */
    static final String PROPERTY_PREFIX = "aurkitu.";

    @Override
    public void apply(Project project) {
        ProviderFactory providers = project.getProviders();
        AurkituExtension extension = project.getExtensions().create(EXTENSION_NAME, AurkituExtension.class);
        Defaults defaults = new Defaults(providers);
        defaults.string(extension.getSchemaName(), "schemaName", project.getName());
        defaults.string(extension.getSchemaNamespace(), "schemaNamespace", "generated.flatbuffers");
        extension.getOutputDirectory().convention(defaults.directory(project, "outputDir",
                project.getLayout().getBuildDirectory().dir("aurkitu/schemas")));
        defaults.list(extension.getSpecifiedPackages(), "specifiedPackages");
        defaults.bool(extension.getValidate(), "validate", true);
        defaults.bool(extension.getIgnoreStaticMembers(), "ignoreStaticMembers", true);
        defaults.string(extension.getFileIdentifier(), "fileIdentifier", null);
        defaults.list(extension.getIncludes(), "includes");
        defaults.bool(extension.getMapCommonTypes(), "mapCommonTypes", true);
        defaults.string(extension.getFlatcPath(), "flatcPath", null);
        defaults.bool(extension.getCacheEnabled(), "cacheEnabled", false);
        defaults.bool(extension.getSnakeCaseFields(), "snakeCaseFields", true);
        defaults.bool(extension.getSnakeCaseAcronyms(), "snakeCaseAcronyms", true);
        defaults.bool(extension.getTypedMapKeys(), "typedMapKeys", false);
        defaults.bool(extension.getGenerateVersion(), "generateVersion", false);
        defaults.bool(extension.getFailOnValidationIssues(), "failOnValidationIssues", false);
        defaults.bool(extension.getOptionalScalars(), "optionalScalars", false);
        defaults.string(extension.getFileExtension(), "fileExtension", null);

        // Every schema task, the plugin's and any the build registers: conventions, so the task's own settings win.
        project.getTasks().withType(GenerateFlatBuffersSchema.class).configureEach(t -> {
            t.setGroup("build");
            if (t.getDescription() == null) {
                t.setDescription("Generates a FlatBuffers schema (.fbs) from the annotated classes.");
            }
            t.getSchemaName().convention(extension.getSchemaName());
            t.getSchemaNamespace().convention(extension.getSchemaNamespace());
            // The plugin's task writes where aurkitu { } says; others get their own directory, because tasks
            // sharing an output directory cannot be cached.
            t.getOutputDirectory().convention(TASK_NAME.equals(t.getName())
                    ? extension.getOutputDirectory()
                    : project.getLayout().getBuildDirectory().dir("aurkitu/" + t.getName()));
            t.getSpecifiedPackages().convention(extension.getSpecifiedPackages());
            t.getValidate().convention(extension.getValidate());
            t.getIgnoreStaticMembers().convention(extension.getIgnoreStaticMembers());
            t.getFileIdentifier().convention(extension.getFileIdentifier());
            t.getIncludes().convention(extension.getIncludes());
            t.getMapCommonTypes().convention(extension.getMapCommonTypes());
            t.getFlatcPath().convention(extension.getFlatcPath());
            t.getCacheEnabled().convention(extension.getCacheEnabled());
            t.getSnakeCaseFields().convention(extension.getSnakeCaseFields());
            t.getSnakeCaseAcronyms().convention(extension.getSnakeCaseAcronyms());
            t.getTypedMapKeys().convention(extension.getTypedMapKeys());
            t.getGenerateVersion().convention(extension.getGenerateVersion());
            t.getFailOnValidationIssues().convention(extension.getFailOnValidationIssues());
            t.getOptionalScalars().convention(extension.getOptionalScalars());
            t.getFileExtension().convention(extension.getFileExtension());
        });
        project.getTasks().register(TASK_NAME, GenerateFlatBuffersSchema.class);

        project.getPluginManager().withPlugin("java", java -> {
            SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class)
                    .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            // The Maven plugin scans the compile classpath, which starts with the project's own classes.
            // A convention, so a task can scan something else with classpath.setFrom(...).
            project.getTasks().withType(GenerateFlatBuffersSchema.class).configureEach(t ->
                    t.getClasspath().convention(main.getOutput().getClassesDirs(), main.getCompileClasspath()));
            project.getTasks().named(BasePlugin.ASSEMBLE_TASK_NAME).configure(assemble ->
                    assemble.dependsOn(project.getTasks().withType(GenerateFlatBuffersSchema.class)));
        });
    }

    /** Conventions read from Gradle properties ({@code aurkitu.<name>}), falling back to the plugin's defaults. */
    private record Defaults(ProviderFactory providers) {

        private Provider<String> property(String name) {
            return providers.gradleProperty(PROPERTY_PREFIX + name);
        }

        void string(Property<String> option, String name, String fallback) {
            option.convention(fallback == null ? property(name) : property(name).orElse(fallback));
        }

        void bool(Property<Boolean> option, String name, boolean fallback) {
            option.convention(property(name).map(AurkituPlugin::parseBoolean).orElse(fallback));
        }

        /**
         * Comma-separated, as Maven reads a list user property. Plain ArrayLists: the configuration cache cannot
         * restore the JDK's immutable lists (List.of(), Stream.toList()).
         */
        void list(ListProperty<String> option, String name) {
            option.convention(property(name).map(AurkituPlugin::splitList).orElse(new ArrayList<>()));
        }

        /** Relative to the project directory. Captures only that directory, not the Project (configuration cache). */
        Provider<Directory> directory(Project project, String name, Provider<Directory> fallback) {
            Directory projectDir = project.getLayout().getProjectDirectory();
            return property(name).map(projectDir::dir).orElse(fallback);
        }
    }

    /** As Maven reads a boolean user property: {@code true} (any case, spaces ignored), anything else false. */
    static boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value.trim());
    }

    static List<String> splitList(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
