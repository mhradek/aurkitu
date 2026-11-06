package io.github.mhradek.aurkitu.plugin.core.model;

/**
 * A top-level schema declaration. Each declaration writes itself.
 */
public sealed interface Declaration
        permits CompositeDeclaration, EnumDeclaration, UnionDeclaration {

    TypeName name();

    /** The Java class this was read from (for messages); generated declarations name their owner. */
    String source();

    void write(SchemaWriter out);
}
