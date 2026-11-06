package io.github.mhradek.aurkitu.plugin.core.model;

import java.util.List;

/**
 * {@code union Name { ... }}; each member names a table.
 */
public record UnionDeclaration(TypeName name, String source, String comment, List<String> members)
        implements Declaration {

    public UnionDeclaration {
        members = List.copyOf(members);
    }

    @Override
    public void write(SchemaWriter out) {
        out.comment(comment);
        out.line("union " + name.name() + " {");
        for (String member : members) {
            out.indented(member + ",");
        }
        out.line("}").blankLine();
    }
}
