package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;
import java.util.Optional;

/**
 * A complete schema file.
 *
 * @param namespace      the default namespace
 * @param includes       files to {@code include}
 * @param fileIdentifier 4-character identifier, or null
 * @param fileExtension  binary file extension, or null
 * @param declarations   declarations in the order they are written
 */
public record Schema(String namespace, List<String> includes, String fileIdentifier, String fileExtension,
                     List<Declaration> declarations) {

    public Schema {
        includes = List.copyOf(includes);
        declarations = List.copyOf(declarations);
    }

    public Schema withDeclarations(List<Declaration> replacement) {
        return new Schema(namespace, includes, fileIdentifier, fileExtension, replacement);
    }

    /** The root type: the last declaration marked as root. */
    public Optional<CompositeDeclaration> root() {
        CompositeDeclaration root = null;
        for (Declaration declaration : declarations) {
            if (declaration instanceof CompositeDeclaration composite && composite.root()) {
                root = composite;
            }
        }
        return Optional.ofNullable(root);
    }

    /**
     * Writes the schema file.
     *
     * @param header           first line (without newline)
     * @param includeVersion   whether to write a {@code // @version: <hash>} line; the hash covers
     *                         the body only, so it is independent of the header and the issues
     * @param validationIssues issues to list in a trailing comment block (empty for none)
     */
    public String write(String header, boolean includeVersion, List<String> validationIssues) {
        return write(header, includeVersion, validationIssues, List.of());
    }

    /**
     * As {@link #write(String, boolean, List)}, followed by a block of warnings (empty for none).
     */
    public String write(String header, boolean includeVersion, List<String> validationIssues, List<String> warnings) {
        String body = writeBody();
        StringBuilder out = new StringBuilder(header).append('\n');
        if (includeVersion) {
            out.append("// @version: ").append(version(body)).append('\n');
        }
        out.append(body);
        appendBlock(out, "Validation issues", validationIssues);
        appendBlock(out, "Warnings", warnings);
        return out.toString();
    }

    private static void appendBlock(StringBuilder out, String title, List<String> lines) {
        if (!lines.isEmpty()) {
            out.append("\n// ").append(title).append(":\n");
            for (String line : lines) {
                out.append("// - ").append(line).append('\n');
            }
        }
    }

    /** Names of the user-defined attributes used anywhere, in order of first use; each must be declared. */
    public List<String> customAttributeNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        java.util.function.Consumer<List<FieldAttribute>> collect = attributes -> attributes.stream()
                .filter(FieldAttribute.Custom.class::isInstance)
                .forEach(a -> names.add(((FieldAttribute.Custom) a).name()));
        for (Declaration declaration : declarations) {
            if (declaration instanceof CompositeDeclaration composite) {
                collect.accept(composite.attributes());
                composite.fields().forEach(field -> collect.accept(field.attributes()));
            } else if (declaration instanceof EnumDeclaration enumDeclaration) {
                collect.accept(enumDeclaration.attributes());
            } else if (declaration instanceof UnionDeclaration union) {
                collect.accept(union.attributes());
            }
        }
        return List.copyOf(names);
    }

    /** Content hash of a schema body. */
    public static String version(String body) {
        return Integer.toHexString(body.hashCode());
    }

    /** Everything except the header, version line and validation comments. */
    public String writeBody() {
        SchemaWriter out = new SchemaWriter(namespace);

        // flatc requires includes before any other declaration.
        for (String include : includes) {
            out.line("include \"" + include + "\";");
        }
        if (!includes.isEmpty()) {
            out.blankLine();
        }

        out.namespace(namespace);
        if (fileIdentifier != null && !fileIdentifier.isBlank()) {
            out.line("file_identifier \"" + fileIdentifier + "\";").blankLine();
        }
        if (fileExtension != null && !fileExtension.isBlank()) {
            out.line("file_extension \"" + fileExtension + "\";").blankLine();
        }
        List<String> customAttributes = customAttributeNames();
        for (String attribute : customAttributes) {
            out.line("attribute \"" + attribute + "\";");
        }
        if (!customAttributes.isEmpty()) {
            out.blankLine();
        }

        for (Declaration declaration : declarations) {
            // A namespace statement applies to everything after it, so restore the default straight away.
            String ns = declaration.name().namespace();
            if (ns.equals(namespace)) {
                declaration.write(out);
            } else {
                out.namespace(ns);
                declaration.write(out);
                out.namespace(namespace);
            }
        }

        root().ifPresent(root -> out.line("root_type " + root.name().referenceFrom(namespace) + ";"));
        return out.text();
    }
}
