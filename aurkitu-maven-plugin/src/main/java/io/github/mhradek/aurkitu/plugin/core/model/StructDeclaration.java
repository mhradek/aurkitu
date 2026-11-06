package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code struct Name { ... }}: fixed size, scalars and other structs only.
 *
 * @param root whether the type was marked as the schema's root type
 */
public record StructDeclaration(TypeName name, String source, String comment, List<Field> fields, boolean root)
        implements CompositeDeclaration {

    public StructDeclaration {
        fields = List.copyOf(fields);
    }

    @Override
    public StructDeclaration withFields(List<Field> replacement) {
        return new StructDeclaration(name, source, comment, replacement, root);
    }

    @Override
    public String keyword() {
        return "struct";
    }
}
