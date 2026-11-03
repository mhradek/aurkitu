package io.github.mhradek.aurkitu.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full Mojo integration tests.
 * Instantiates AurkituMojo, sets parameters the same way Maven would,
 * and verifies the plugin descriptor.
 */
class AurkituMojoIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void executeWritesSchemaWithConfiguredOptions() throws Exception {
        AurkituMojo mojo = new AurkituMojo();
        set(mojo, "schemaName", "user");
        set(mojo, "schemaNamespace", "com.example");
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "specifiedPackages", List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"));
        set(mojo, "validate", true);
        set(mojo, "ignoreStaticMembers", true);
        set(mojo, "fileIdentifier", "USER");
        set(mojo, "includes", List.of("shared.fbs"));
        set(mojo, "mapCommonTypes", true);
        set(mojo, "snakeCaseFields", true);
        set(mojo, "generateVersion", true);
        set(mojo, "failOnValidationIssues", false);
        set(mojo, "fileExtension", "bin");
        set(mojo, "cacheEnabled", false);

        mojo.execute();

        Path schema = tempDir.resolve("user.fbs");
        assertTrue(Files.exists(schema), "Schema file should be written");
        String content = Files.readString(schema);
        assertTrue(content.contains("namespace com.example;"));
        assertTrue(content.contains("file_identifier \"USER\";"));
        assertTrue(content.contains("include \"shared.fbs\";"));
        assertTrue(content.contains("// @version:"));
        assertTrue(content.contains("table SampleRootTable"));
        assertTrue(content.contains("nested_map:") || content.contains("base_id:"));
    }

    @Test
    void executeCreatesOutputDirectory() throws Exception {
        Path nested = tempDir.resolve("nested/out");
        assertFalse(Files.exists(nested));

        AurkituMojo mojo = new AurkituMojo();
        set(mojo, "schemaName", "dirtest");
        set(mojo, "schemaNamespace", "test");
        set(mojo, "outputDirectory", nested.toFile());
        set(mojo, "specifiedPackages", List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"));
        set(mojo, "validate", false);
        set(mojo, "ignoreStaticMembers", true);
        set(mojo, "mapCommonTypes", true);
        set(mojo, "snakeCaseFields", true);
        set(mojo, "generateVersion", false);
        set(mojo, "failOnValidationIssues", false);
        set(mojo, "fileExtension", "bin");
        set(mojo, "cacheEnabled", false);

        mojo.execute();

        assertTrue(Files.exists(nested.resolve("dirtest.fbs")));
    }

    @Test
    void pluginDescriptorHasCorrectCoordinatesAndGoal() throws Exception {
        // After compile, plugin.xml is generated under target/classes
        Path pluginXml = Path.of("target/classes/META-INF/maven/plugin.xml");
        if (!Files.exists(pluginXml)) {
            // Fallback for when tests run from a different working directory
            pluginXml = Path.of("aurkitu-maven-plugin/target/classes/META-INF/maven/plugin.xml");
        }
        assertTrue(Files.exists(pluginXml), "plugin.xml should exist after compile: " + pluginXml.toAbsolutePath());

        String xml = Files.readString(pluginXml);
        assertTrue(xml.contains("<groupId>io.github.mhradek</groupId>"));
        assertTrue(xml.contains("<artifactId>aurkitu-maven-plugin</artifactId>"));
        assertTrue(xml.contains("<goal>build-schema</goal>"));
        assertTrue(xml.contains("<name>schemaName</name>"));
        assertTrue(xml.contains("<name>snakeCaseFields</name>"));
        assertTrue(xml.contains("<name>generateVersion</name>"));
        assertTrue(xml.contains("<name>failOnValidationIssues</name>"));
        assertTrue(xml.contains("<name>flatcPath</name>"));
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
