package io.github.mhradek.aurkitu.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that are about the plugin as a product rather than one build: that its options match the Maven plugin's,
 * and that it works when resolved the way users get it, by plugin id and version from a repository.
 */
class PluginParityAndPublishingTest {

    private static final String VERSION = System.getProperty("aurkitu.version");
    private static final String LOCAL_REPO = Path.of(System.getProperty("maven.repo.local")).toUri().toString();
    private static final Path MAVEN_PLUGIN_JAR = Path.of(System.getProperty("aurkitu.mavenPluginJar"));
    private static final String PLUGIN_REPO = Path.of(System.getProperty("aurkitu.pluginRepo")).toUri().toString();

    /** Maven-only parameters: the project is Maven's own object, not an option. */
    private static final Set<String> MAVEN_ONLY = Set.of("project");

    /** Options whose Gradle default differs on purpose (Gradle's build directory; schemaName defaults to the project name). */
    private static final Map<String, String> GRADLE_DEFAULTS = Map.of(
            "outputDirectory", "build/aurkitu/schemas",
            "schemaName", "parity");

    @TempDir
    Path project;

    /**
     * Every Maven plugin parameter is an {@code aurkitu { }} option with the same name and default, and the other
     * way round: an option added to one plugin and not the other fails here.
     */
    @Test
    void optionsAndDefaultsMatchTheMavenPlugin() throws Exception {
        Map<String, String> maven = mavenParameters();
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'parity'\n");
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.mhradek.aurkitu'
                }
                tasks.register('printOptions') {
                    def options = aurkitu.class.methods
                            .findAll { it.name.startsWith('get') && it.parameterCount == 0
                                    && org.gradle.api.provider.Provider.isAssignableFrom(it.returnType) }
                            .collectEntries { m ->
                                def value = m.invoke(aurkitu).getOrNull()
                                if (value instanceof org.gradle.api.file.Directory) {
                                    value = project.projectDir.toPath().relativize(value.asFile.toPath()).toString()
                                } else if (value instanceof List) {
                                    value = value.join(',')
                                }
                                [(m.name.substring(3, 4).toLowerCase() + m.name.substring(4)): value]
                            }
                    doLast {
                        options.each { name, value -> println "OPTION ${name}=${value == null ? '' : value}" }
                    }
                }
                """);

        BuildResult result = GradleRunner.create().withProjectDir(project.toFile()).withPluginClasspath()
                .withArguments("printOptions", "-q").build();

        Map<String, String> gradle = new TreeMap<>();
        result.getOutput().lines().filter(l -> l.startsWith("OPTION ")).forEach(l -> {
            String[] kv = l.substring("OPTION ".length()).split("=", 2);
            gradle.put(kv[0], kv[1]);
        });
        Map<String, String> expected = new TreeMap<>(maven);
        MAVEN_ONLY.forEach(expected::remove);
        expected.putAll(GRADLE_DEFAULTS);
        assertEquals(expected, gradle, "aurkitu { } options and defaults (left: the Maven plugin's)");
    }

    /**
     * The plugin resolved by id and version from a Maven repository, as from the Gradle Plugin Portal, rather than
     * injected by TestKit: catches a published POM that does not resolve (e.g. a wrong aurkitu-core version).
     */
    @Test
    void thePublishedPluginResolvesByIdAndVersion() throws Exception {
        String mirror = System.getProperty("aurkitu.centralMirror");
        String central = mirror == null ? "mavenCentral()" : "maven { url = '" + mirror + "' }";
        Files.writeString(project.resolve("settings.gradle"), """
                pluginManagement {
                    repositories {
                        maven { url = '%s' }
                        maven { url = '%s' }
                        %s
                    }
                }
                rootProject.name = 'published'
                """.formatted(PLUGIN_REPO, LOCAL_REPO, central));
        Files.writeString(project.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.mhradek.aurkitu' version '%s'
                }
                repositories {
                    maven { url = '%s' }
                    %s
                }
                dependencies { implementation 'io.github.mhradek:aurkitu-annotations:%s' }
                aurkitu { specifiedPackages = ['com.example'] }
                """.formatted(VERSION, LOCAL_REPO, central, VERSION));
        Path source = Files.createDirectories(project.resolve("src/main/java/com/example"));
        Files.writeString(source.resolve("Greeting.java"), """
                package com.example;

                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

                @FlatBufferTable(rootType = true)
                public class Greeting {
                    private String text;
                }
                """);

        // No withPluginClasspath(): the plugin and its dependencies come from the repositories above.
        BuildResult result = GradleRunner.create().withProjectDir(project.toFile())
                .withArguments(":generateFlatBuffersSchema", "--stacktrace").forwardOutput().build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateFlatBuffersSchema").getOutcome());
        String schema = Files.readString(project.resolve("build/aurkitu/schemas/published.fbs"));
        assertTrue(schema.contains("table Greeting {\n  text:string;\n}"), schema);
    }

    /** The {@code build-schema} goal's parameters and defaults, from the Maven plugin's descriptor. */
    private static Map<String, String> mavenParameters() throws Exception {
        assertTrue(Files.exists(MAVEN_PLUGIN_JAR), "the Maven plugin is installed (mvn install): " + MAVEN_PLUGIN_JAR);
        Document descriptor;
        try (JarFile jar = new JarFile(MAVEN_PLUGIN_JAR.toFile());
             InputStream in = jar.getInputStream(jar.getEntry("META-INF/maven/plugin.xml"))) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            descriptor = factory.newDocumentBuilder().parse(in);
        }
        Element mojo = null;
        NodeList mojos = descriptor.getElementsByTagName("mojo");
        for (int i = 0; i < mojos.getLength(); i++) {
            Element m = (Element) mojos.item(i);
            if ("build-schema".equals(m.getElementsByTagName("goal").item(0).getTextContent())) {
                mojo = m;
            }
        }
        assertNotNull(mojo, "build-schema goal in plugin.xml");

        Map<String, String> parameters = new TreeMap<>();
        NodeList names = ((Element) mojo.getElementsByTagName("parameters").item(0)).getElementsByTagName("name");
        for (int i = 0; i < names.getLength(); i++) {
            parameters.put(names.item(i).getTextContent(), "");
        }
        Element configuration = (Element) mojo.getElementsByTagName("configuration").item(0);
        for (Node n = configuration.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.hasAttribute("default-value")) {
                parameters.put(e.getTagName(), e.getAttribute("default-value"));
            }
        }
        return parameters;
    }
}
