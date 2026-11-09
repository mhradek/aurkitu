package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code table Name { ... }}.
 *
 * @param root       whether the type was marked as the schema's root type
 * @param attributes attributes written after the name
 */
public record TableDeclaration(TypeName name, String source, String comment, List<Field> fields, boolean root,
                               List<FieldAttribute> attributes) implements CompositeDeclaration {

    public TableDeclaration {
        fields = List.copyOf(fields);
        attributes = List.copyOf(attributes);
    }

    public TableDeclaration(TypeName name, String source, String comment, List<Field> fields, boolean root) {
        this(name, source, comment, fields, root, List.of());
    }

    @Override
    public TableDeclaration withFields(List<Field> replacement) {
        return new TableDeclaration(name, source, comment, replacement, root, attributes);
    }

    @Override
    public TableDeclaration withAttributes(List<FieldAttribute> replacement) {
        return new TableDeclaration(name, source, comment, fields, root, replacement);
    }

    @Override
    public String keyword() {
        return "table";
    }
}
