package io.github.mhradek.aurkitu.gradle;

import io.github.mhradek.aurkitu.plugin.core.AurkituVersion;
import io.github.mhradek.aurkitu.plugin.core.Flatc;
import io.github.mhradek.aurkitu.plugin.core.SchemaGenerationException;
import io.github.mhradek.aurkitu.plugin.core.SchemaGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a FlatBuffers schema ({@code <schemaName>.fbs}) from the annotated classes on {@link #getClasspath()}:
 * by default, the {@code main} source set's classes and its compile classpath, so annotated classes in
 * dependencies are found too. The same generation as the Maven plugin's {@code build-schema} goal.
 */
@CacheableTask
public abstract class GenerateFlatBuffersSchema extends DefaultTask {

    /**
     * Classes to scan. {@code @Classpath}, not {@code @CompileClasspath}: private fields are part of the schema,
     * so a change to one must make the task run again.
     */
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Input
    public abstract Property<String> getSchemaName();

    @Input
    public abstract Property<String> getSchemaNamespace();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    public abstract ListProperty<String> getSpecifiedPackages();

    @Input
    public abstract Property<Boolean> getValidate();

    @Input
    public abstract Property<Boolean> getIgnoreStaticMembers();

    @Input
    @Optional
    public abstract Property<String> getFileIdentifier();

    @Input
    public abstract ListProperty<String> getIncludes();

    @Input
    public abstract Property<Boolean> getMapCommonTypes();

    @Input
    @Optional
    public abstract Property<String> getFlatcPath();

    @Input
    public abstract Property<Boolean> getCacheEnabled();

    @Input
    public abstract Property<Boolean> getSnakeCaseFields();

    @Input
    public abstract Property<Boolean> getSnakeCaseAcronyms();

    @Input
    public abstract Property<Boolean> getTypedMapKeys();

    @Input
    public abstract Property<Boolean> getGenerateVersion();

    @Input
    public abstract Property<Boolean> getFailOnValidationIssues();

    @Input
    public abstract Property<Boolean> getOptionalScalars();

    @Input
    @Optional
    public abstract Property<String> getFileExtension();

    @TaskAction
    public void generate() {
        List<String> classpath = getClasspath().getFiles().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
        if (classpath.isEmpty()) {
            // An empty classpath would make the scanner fall back to Gradle's own classes.
            throw new GradleException("Aurkitu: nothing to scan; the task's classpath is empty");
        }
        String schemaName = getSchemaName().get();
        String namespace = getSchemaNamespace().get();
        File outputDirectory = getOutputDirectory().get().getAsFile();
        boolean validate = getValidate().get();
        getLogger().info("Aurkitu {} - generating schema '{}' in namespace '{}'", AurkituVersion.VERSION, schemaName, namespace);

        SchemaGenerator.Config config = new SchemaGenerator.Config(
                getSpecifiedPackages().get(), validate, getIgnoreStaticMembers().get(), getFileIdentifier().getOrNull(),
                getIncludes().get(), getMapCommonTypes().get(), getCacheEnabled().get(), getSnakeCaseFields().get(),
                getGenerateVersion().get(), getFailOnValidationIssues().get(), getFileExtension().getOrNull(),
                classpath, getOptionalScalars().get())
                .withSnakeCaseAcronyms(getSnakeCaseAcronyms().get())
                .withTypedMapKeys(getTypedMapKeys().get());
        try {
            new SchemaGenerator(config).generate(schemaName, namespace, outputDirectory);
            String flatc = getFlatcPath().getOrNull();
            if (validate && flatc != null && !flatc.isBlank()) {
                Flatc.validate(flatc, new File(outputDirectory, schemaName + ".fbs"));
            }
        } catch (SchemaGenerationException e) {
            throw new GradleException(e.getMessage(), e);
        }
    }
}
