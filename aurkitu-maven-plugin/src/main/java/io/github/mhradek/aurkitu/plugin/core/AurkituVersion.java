package io.github.mhradek.aurkitu.plugin.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Build-time metadata. The version comes from a resource filtered with
 * {@code ${project.version}}, so the POM is the single source of truth.
 */
public final class AurkituVersion {

    static final String RESOURCE = "/io/github/mhradek/aurkitu/plugin/aurkitu.properties";

    /** The plugin version, or {@code "unknown"} if the resource is missing or unfiltered. */
    public static final String VERSION = load(AurkituVersion.class.getResourceAsStream(RESOURCE));

    private AurkituVersion() {}

    static String load(InputStream in) {
        if (in == null) {
            return "unknown";
        }
        try (in) {
            Properties props = new Properties();
            props.load(in);
            String version = props.getProperty("version", "").trim();
            return version.isEmpty() || version.startsWith("${") ? "unknown" : version;
        } catch (IOException e) {
            return "unknown";
        }
    }

    /** First line of every generated schema. */
    static String schemaHeader() {
        return "// Aurkitu " + VERSION + " automatically generated FlatBuffer schema\n";
    }
}
