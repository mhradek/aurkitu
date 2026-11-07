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

        String content = Files.readString(tempDir.resolve("user.fbs"));
        assertTrue(content.matches("(?s)// Aurkitu [^\n]+\n// @version: [0-9a-f]{1,8}\ninclude \"shared.fbs\";\n\n"
                + "namespace com\\.example;\n\nfile_identifier \"USER\";\n\nfile_extension \"bin\";\n\n.*"), content);
        assertTrue(content.contains("  base_id:long;\n"), content);
        assertTrue(content.contains("  created:string;\n"), content);
        assertTrue(content.contains("  flag:bool;\n"), "optionalScalars is off:\n" + content);
        assertFalse(content.contains("instances"), content);
        assertTrue(content.contains("// Validation issues:\n"), content);
    }

    @Test
    void everyNonDefaultSettingReachesTheGenerator() throws Exception {
        // Each flag flipped from its default; a swapped constructor argument would show here.
        AurkituMojo mojo = new AurkituMojo();
        set(mojo, "schemaName", "flipped");
        set(mojo, "schemaNamespace", "com.flipped");
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "specifiedPackages", List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"));
        set(mojo, "validate", false);
        set(mojo, "ignoreStaticMembers", false);
        set(mojo, "mapCommonTypes", false);
        set(mojo, "snakeCaseFields", false);
        set(mojo, "generateVersion", false);
        set(mojo, "failOnValidationIssues", true); // no effect while validate is off
        set(mojo, "optionalScalars", true);
        set(mojo, "cacheEnabled", true);

        mojo.execute();

        String content = Files.readString(tempDir.resolve("flipped.fbs"));
        assertTrue(content.contains("table SampleWithCommonTypes {\n  flag:bool = null;\n  count:int = null;\n"
                + "  tags:[string];\n  numbers:[int];\n  bytes:[ubyte];\n  instances:int;\n}"), content);
        assertTrue(content.contains("  baseId:long;\n"), content);
        assertFalse(content.contains("// @version:"), content);
        assertFalse(content.contains("// Validation issues:"), content);
        assertTrue(content.endsWith("root_type SampleRootTable;\n"), content);
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
    void executeFailsWhenFlatcPathIsInvalid() throws Exception {
        AurkituMojo mojo = new AurkituMojo();
        set(mojo, "schemaName", "badflatc");
        set(mojo, "schemaNamespace", "test");
        set(mojo, "outputDirectory", tempDir.toFile());
        set(mojo, "specifiedPackages", List.of("io.github.mhradek.aurkitu.plugin.core.testmodels"));
        set(mojo, "validate", true);
        set(mojo, "ignoreStaticMembers", true);
        set(mojo, "mapCommonTypes", true);
        set(mojo, "snakeCaseFields", true);
        set(mojo, "generateVersion", false);
        set(mojo, "failOnValidationIssues", false);
        set(mojo, "cacheEnabled", false);
        set(mojo, "flatcPath", "/nonexistent/flatc");

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void pluginDescriptorHasCorrectCoordinatesAndGoal() throws Exception {
        Path pluginXml = Path.of("target/classes/META-INF/maven/plugin.xml");
        if (!Files.exists(pluginXml)) {
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
        assertTrue(xml.contains("<name>optionalScalars</name>"));
        // Defaults as documented in the README.
        for (String line : List.of(
                "<cacheEnabled implementation=\"boolean\" default-value=\"false\">${aurkitu.cacheEnabled}</cacheEnabled>",
                "<failOnValidationIssues implementation=\"boolean\" default-value=\"false\">${aurkitu.failOnValidationIssues}</failOnValidationIssues>",
                "<generateVersion implementation=\"boolean\" default-value=\"false\">${aurkitu.generateVersion}</generateVersion>",
                "<ignoreStaticMembers implementation=\"boolean\" default-value=\"true\">${aurkitu.ignoreStaticMembers}</ignoreStaticMembers>",
                "<mapCommonTypes implementation=\"boolean\" default-value=\"true\">${aurkitu.mapCommonTypes}</mapCommonTypes>",
                "<optionalScalars implementation=\"boolean\" default-value=\"false\">${aurkitu.optionalScalars}</optionalScalars>",
                "<outputDirectory implementation=\"java.io.File\" default-value=\"${project.build.directory}/aurkitu/schemas\">${aurkitu.outputDir}</outputDirectory>",
                "<schemaNamespace implementation=\"java.lang.String\" default-value=\"generated.flatbuffers\">${aurkitu.schemaNamespace}</schemaNamespace>",
                "<snakeCaseFields implementation=\"boolean\" default-value=\"true\">${aurkitu.snakeCaseFields}</snakeCaseFields>",
                "<validate implementation=\"boolean\" default-value=\"true\">${aurkitu.validate}</validate>")) {
            assertTrue(xml.contains(line), line);
        }
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
