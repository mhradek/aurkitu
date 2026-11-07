package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A table or struct field.
 *
 * @param javaName     the Java field name, used in validation messages
 * @param name         the schema field name (e.g. snake_case)
 * @param type         the field type
 * @param defaultValue default value, or empty for none
 * @param comment      comment line written above the field, or empty for none
 * @param attributes   attributes, written in list order
 * @param declaredIn   the Java class declaring the field (a superclass for inherited fields), or empty
 */
public record Field(String javaName, String name, SchemaType type, String defaultValue, String comment,
                    List<FieldAttribute> attributes, String declaredIn) {

    public Field {
        attributes = List.copyOf(attributes);
    }

    public Field(String javaName, String name, SchemaType type, String defaultValue, String comment,
                 List<FieldAttribute> attributes) {
        this(javaName, name, type, defaultValue, comment, attributes, "");
    }

    public static Field of(String name, SchemaType type) {
        return new Field(name, name, type, "", "", List.of());
    }

    public Field withAttributes(List<FieldAttribute> replacement) {
        return new Field(javaName, name, type, defaultValue, comment, replacement, declaredIn);
    }

    public Field withType(SchemaType replacement) {
        return new Field(javaName, name, replacement, defaultValue, comment, attributes, declaredIn);
    }

    public Field withDefaultValue(String replacement) {
        return new Field(javaName, name, type, replacement, comment, attributes, declaredIn);
    }

    public boolean has(Class<? extends FieldAttribute> kind) {
        return attributes.stream().anyMatch(kind::isInstance);
    }

    public void write(SchemaWriter out) {
        if (!comment.isBlank()) {
            out.indented("// " + comment);
        }
        StringBuilder line = new StringBuilder(name).append(':').append(type.write(out.scope()));
        if (!defaultValue.isBlank()) {
            line.append(" = ").append(defaultValue);
        }
        if (!attributes.isEmpty()) {
            line.append(" (")
                    .append(attributes.stream().map(FieldAttribute::write).collect(Collectors.joining(", ")))
                    .append(')');
        }
        out.indented(line.append(';').toString());
    }
}
