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

    /** Writes optional documentation as {@code ///} lines, which flatc copies into generated code. */
    public SchemaWriter comment(String comment) {
        return docLines(comment, "");
    }

    /** As {@link #comment(String)}, indented (for fields, enum values and members). */
    public SchemaWriter indentedComment(String comment) {
        return docLines(comment, INDENT);
    }

    private SchemaWriter docLines(String comment, String indent) {
        if (!comment.isBlank()) {
            for (String line : comment.strip().split("\\R")) {
                line(indent + ("/// " + line.strip()).strip());
            }
        }
        return this;
    }

    /** {@code " (a, b)"} for attributes after a declaration name, or empty. */
    public static String attributes(java.util.List<FieldAttribute> attributes) {
        return attributes.isEmpty() ? ""
                : " (" + String.join(", ", attributes.stream().map(FieldAttribute::write).toList()) + ")";
    }

    public String text() {
        return out.toString();
    }
}
