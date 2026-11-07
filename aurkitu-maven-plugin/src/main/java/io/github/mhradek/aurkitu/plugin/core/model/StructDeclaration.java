package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code struct Name { ... }}: fixed size, scalars and other structs only.
 *
 * @param root       whether the type was marked as the schema's root type
 * @param attributes attributes written after the name
 */
public record StructDeclaration(TypeName name, String source, String comment, List<Field> fields, boolean root,
                                List<FieldAttribute> attributes) implements CompositeDeclaration {

    public StructDeclaration {
        fields = List.copyOf(fields);
        attributes = List.copyOf(attributes);
    }

    public StructDeclaration(TypeName name, String source, String comment, List<Field> fields, boolean root) {
        this(name, source, comment, fields, root, List.of());
    }

    @Override
    public StructDeclaration withFields(List<Field> replacement) {
        return new StructDeclaration(name, source, comment, replacement, root, attributes);
    }

    @Override
    public StructDeclaration withAttributes(List<FieldAttribute> replacement) {
        return new StructDeclaration(name, source, comment, fields, root, replacement);
    }

    @Override
    public String keyword() {
        return "struct";
    }
}
