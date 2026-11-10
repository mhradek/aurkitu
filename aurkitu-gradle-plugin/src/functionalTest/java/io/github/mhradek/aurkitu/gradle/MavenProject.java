package io.github.mhradek.aurkitu.gradle;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One of the Maven plugin's end-to-end projects: the Aurkitu plugin configuration in its POM, as Gradle (Groovy
 * DSL) configuration, and whether the Maven build is expected to fail ({@code invoker.properties}).
 */
record MavenProject(List<String> lines, String schemaName, List<String> requiredEnv, boolean expectFailure) {

    private static final Pattern ENV = Pattern.compile("\\$\\{env\\.(\\w+)}");

    static MavenProject read(Path dir) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document pom = factory.newDocumentBuilder().parse(dir.resolve("pom.xml").toFile());
        Element configuration = aurkituConfiguration(pom);

        List<String> lines = new ArrayList<>();
        List<String> requiredEnv = new ArrayList<>();
        String schemaName = null;
        for (Element option : children(configuration)) {
            String name = option.getTagName();
            List<Element> items = children(option);
            String value;
            if (!items.isEmpty()) {
                List<String> quoted = items.stream().map(i -> groovyString(i.getTextContent().trim(), requiredEnv)).toList();
                value = "[" + String.join(", ", quoted) + "]";
            } else {
                String text = option.getTextContent().trim();
                value = "true".equals(text) || "false".equals(text) ? text : groovyString(text, requiredEnv);
                if ("schemaName".equals(name)) {
                    schemaName = text;
                }
            }
            lines.add("    " + name + " = " + value);
        }

        Properties invoker = new Properties();
        Path invokerFile = dir.resolve("invoker.properties");
        if (Files.exists(invokerFile)) {
            try (var in = Files.newInputStream(invokerFile)) {
                invoker.load(in);
            }
        }
        return new MavenProject(lines, schemaName, requiredEnv, "failure".equals(invoker.getProperty("invoker.buildResult")));
    }

    String groovyConfiguration() {
        return String.join("\n", lines);
    }

    /** False when an environment variable the POM uses (e.g. FLATC_PATH) is not set, as Maven's selector skips it. */
    boolean runnable() {
        return requiredEnv.stream().allMatch(v -> System.getenv(v) != null && !System.getenv(v).isBlank());
    }

    /** {@code ${env.X}} becomes {@code System.getenv('X')}; anything else a single-quoted string. */
    private static String groovyString(String text, List<String> requiredEnv) {
        Matcher env = ENV.matcher(text);
        if (env.matches()) {
            requiredEnv.add(env.group(1));
            return "System.getenv('" + env.group(1) + "')";
        }
        return "'" + text.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    /** The {@code <configuration>} of the plugin under test ({@code @project.artifactId@} in the IT POMs). */
    private static Element aurkituConfiguration(Document pom) {
        NodeList plugins = pom.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            NodeList artifactIds = plugin.getElementsByTagName("artifactId");
            if (artifactIds.getLength() > 0 && artifactIds.item(0).getTextContent().contains("@project.artifactId@")) {
                return (Element) plugin.getElementsByTagName("configuration").item(0);
            }
        }
        throw new IllegalStateException("No Aurkitu plugin configuration in the POM");
    }

    private static List<Element> children(Element parent) {
        List<Element> elements = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                elements.add(e);
            }
        }
        return elements;
    }
}
