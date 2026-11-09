package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code enum Name : underlying (attributes) { ... }}.
 *
 * @param values        constants in the order they are written
 * @param valuesProblem why explicit {@code @FlatBufferEnumTypeField} values could not be
 *                      used (values are then implicit), or null
 * @param attributes    attributes written after the name (bit_flags, custom)
 */
public record EnumDeclaration(TypeName name, String source, String comment, ScalarKind underlying,
                              List<Value> values, String valuesProblem, List<FieldAttribute> attributes)
        implements Declaration {

    public EnumDeclaration {
        values = List.copyOf(values);
        attributes = List.copyOf(attributes);
    }

    public EnumDeclaration(TypeName name, String source, String comment, ScalarKind underlying,
                           List<Value> values, String valuesProblem) {
        this(name, source, comment, underlying, values, valuesProblem, List.of());
    }

    /**
     * An enum constant; {@code value} is null when implicit (declaration order).
     *
     * @param comment documentation written above the constant, or empty
     */
    public record Value(String name, Long value, String comment) {
        public Value(String name, Long value) {
            this(name, value, "");
        }
    }

    public EnumDeclaration withAttributes(List<FieldAttribute> replacement) {
        return new EnumDeclaration(name, source, comment, underlying, values, valuesProblem, replacement);
    }

    public boolean bitFlags() {
        return attributes.stream().anyMatch(FieldAttribute.BitFlags.class::isInstance);
    }

    @Override
    public void write(SchemaWriter out) {
        out.comment(comment);
        out.line("enum " + name.name() + " : " + underlying.keyword() + SchemaWriter.attributes(attributes) + " {");
        for (Value v : values) {
            out.indentedComment(v.comment());
            out.indented(v.value() == null ? v.name() + "," : v.name() + " = " + format(v.value()) + ",");
        }
        out.line("}").blankLine();
    }

    /** Values are held as Java longs; a ulong above Long.MAX_VALUE is stored in two's complement. */
    public String format(long value) {
        return underlying == ScalarKind.ULONG ? Long.toUnsignedString(value) : Long.toString(value);
    }
}
