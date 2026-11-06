package io.github.mhradek.aurkitu.plugin.core.model;

/**
 * A declared (or referenced) schema type: its namespace and its name within that namespace.
 */
public record TypeName(String namespace, String name) {

    /** {@code namespace.name}, or just the name when there is no namespace. */
    public String qualified() {
        return namespace.isEmpty() ? name : namespace + "." + name;
    }

    /** How code written inside {@code scope} refers to this type: unqualified within its own namespace. */
    public String referenceFrom(String scope) {
        return namespace.equals(scope) ? name : qualified();
    }
}
