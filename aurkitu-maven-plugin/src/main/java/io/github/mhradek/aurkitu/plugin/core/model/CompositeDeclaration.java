package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * A declaration made of fields: a table or a struct.
 */
public sealed interface CompositeDeclaration extends Declaration
        permits TableDeclaration, StructDeclaration {

    String comment();

    List<Field> fields();

    /** Whether the type was marked as the schema's root type. */
    boolean root();

    /** Attributes written after the name (original_order, force_align, custom). */
    List<FieldAttribute> attributes();

    CompositeDeclaration withAttributes(List<FieldAttribute> attributes);

    CompositeDeclaration withFields(List<Field> fields);

    /** {@code table} or {@code struct}. */
    String keyword();

    @Override
    default void write(SchemaWriter out) {
        out.comment(comment());
        out.line(keyword() + " " + name().name() + SchemaWriter.attributes(attributes()) + " {");
        for (Field field : fields()) {
            field.write(out);
        }
        out.line("}").blankLine();
    }
}
