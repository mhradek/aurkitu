package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code table Name { ... }}.
 *
 * @param root whether the type was marked as the schema's root type
 */
public record TableDeclaration(TypeName name, String source, String comment, List<Field> fields, boolean root)
        implements CompositeDeclaration {

    public TableDeclaration {
        fields = List.copyOf(fields);
    }

    @Override
    public TableDeclaration withFields(List<Field> replacement) {
        return new TableDeclaration(name, source, comment, replacement, root);
    }

    @Override
    public String keyword() {
        return "table";
    }
}
