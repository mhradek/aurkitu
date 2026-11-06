package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code enum Name : underlying { ... }}.
 *
 * @param values             constants in the order they are written
 * @param unreadValueField   name of a {@code @FlatBufferEnumTypeField} whose values could not
 *                           be read (values are then implicit), or null
 */
public record EnumDeclaration(TypeName name, String source, String comment, ScalarKind underlying,
                              List<Value> values, String unreadValueField) implements Declaration {

    public EnumDeclaration {
        values = List.copyOf(values);
    }

    /** An enum constant; {@code value} is null when implicit (declaration order). */
    public record Value(String name, Long value) {}

    @Override
    public void write(SchemaWriter out) {
        out.comment(comment);
        out.line("enum " + name.name() + " : " + underlying.keyword() + " {");
        for (Value v : values) {
            out.indented(v.value() == null ? v.name() + "," : v.name() + " = " + format(v.value()) + ",");
        }
        out.line("}").blankLine();
    }

    /** Values are held as Java longs; a ulong above Long.MAX_VALUE is stored in two's complement. */
    public String format(long value) {
        return underlying == ScalarKind.ULONG ? Long.toUnsignedString(value) : Long.toString(value);
    }
}
