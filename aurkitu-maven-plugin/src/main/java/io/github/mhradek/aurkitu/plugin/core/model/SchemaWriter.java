package io.github.mhradek.aurkitu.plugin.core.model;

/**
 * Accumulates schema text. Owns line endings and indentation, and tracks the namespace
 * currently in effect so that references can be written relative to it.
 */
public final class SchemaWriter {

    private static final String INDENT = "  ";

    private final StringBuilder out = new StringBuilder();
    private String scope;

    public SchemaWriter(String scope) {
        this.scope = scope;
    }

    /** The namespace declarations are currently being written in. */
    public String scope() {
        return scope;
    }

    public SchemaWriter line(String text) {
        out.append(text).append('\n');
        return this;
    }

    public SchemaWriter blankLine() {
        out.append('\n');
        return this;
    }

    public SchemaWriter indented(String text) {
        return line(INDENT + text);
    }

    /** Writes a {@code namespace} statement and makes it the current scope. */
    public SchemaWriter namespace(String namespace) {
        scope = namespace;
        return line("namespace " + namespace + ";").blankLine();
    }

    /** Writes an optional comment line. */
    public SchemaWriter comment(String comment) {
        if (!comment.isBlank()) {
            line("// " + comment);
        }
        return this;
    }

    public String text() {
        return out.toString();
    }
}
