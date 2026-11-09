package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code union Name (attributes) { ... }}; each member holds a table (flatc also accepts structs).
 */
public record UnionDeclaration(TypeName name, String source, String comment, List<Member> members,
                               List<FieldAttribute> attributes) implements Declaration {

    public UnionDeclaration {
        members = List.copyOf(members);
        attributes = List.copyOf(attributes);
    }

    public UnionDeclaration(TypeName name, String source, String comment, List<Member> members) {
        this(name, source, comment, members, List.of());
    }

    /** A union whose members are tables in its own namespace, named as given. */
    public static UnionDeclaration of(TypeName name, String source, String comment, List<String> tables) {
        return new UnionDeclaration(name, source, comment,
                tables.stream().map(t -> new Member(null, new TypeName(name.namespace(), t), "")).toList());
    }

    /**
     * A member: {@code Type} or, with an alias, {@code Alias:Type}.
     *
     * @param alias   the member's name when it differs from the type's (e.g. {@code Start}), or null
     * @param type    the member's table
     * @param comment documentation written above the member, or empty
     */
    public record Member(String alias, TypeName type, String comment) {

        /** The name the member is known by: its alias, or the type's name. */
        public String label() {
            return alias != null ? alias : type.name();
        }

        String write(String scope) {
            String reference = type.referenceFrom(scope);
            return alias != null ? alias + ":" + reference : reference;
        }
    }

    public UnionDeclaration withAttributes(List<FieldAttribute> replacement) {
        return new UnionDeclaration(name, source, comment, members, replacement);
    }

    @Override
    public void write(SchemaWriter out) {
        out.comment(comment);
        out.line("union " + name.name() + SchemaWriter.attributes(attributes) + " {");
        for (Member member : members) {
            out.indentedComment(member.comment());
            out.indented(member.write(out.scope()) + ",");
        }
        out.line("}").blankLine();
    }
}
