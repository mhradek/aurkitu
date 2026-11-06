package io.github.mhradek.aurkitu.plugin.core.model;

/**
 * The type of a field. Each kind writes its own syntax; references are written
 * relative to the namespace they are written in.
 */
public sealed interface SchemaType {

    /** Schema syntax for this type when written inside {@code scope}. */
    String write(String scope);

    /** A scalar such as {@code int} or {@code ubyte}. */
    record Scalar(ScalarKind kind) implements SchemaType {
        @Override
        public String write(String scope) {
            return kind.keyword();
        }
    }

    /** {@code string}. */
    record StringType() implements SchemaType {
        public static final StringType INSTANCE = new StringType();

        @Override
        public String write(String scope) {
            return "string";
        }
    }

    /** A vector, {@code [element]}. */
    record Vector(SchemaType element) implements SchemaType {
        @Override
        public String write(String scope) {
            return "[" + element.write(scope) + "]";
        }
    }

    /** A fixed-length array (structs only), {@code [element:length]}. */
    record FixedArray(SchemaType element, int length) implements SchemaType {
        @Override
        public String write(String scope) {
            return "[" + element.write(scope) + ":" + length + "]";
        }
    }

    /**
     * A reference to a table, struct, enum or union. With {@code alwaysQualified}
     * (the {@code useFullName} option) the fully qualified name is written everywhere.
     */
    record Reference(TypeName target, boolean alwaysQualified) implements SchemaType {
        public Reference(TypeName target) {
            this(target, false);
        }

        @Override
        public String write(String scope) {
            return alwaysQualified ? target.qualified() : target.referenceFrom(scope);
        }
    }

    /** A Java type with no schema equivalent. The validator removes such fields before writing. */
    record Unsupported(String javaType) implements SchemaType {
        @Override
        public String write(String scope) {
            throw new IllegalStateException("Unsupported type cannot be written: " + javaType);
        }
    }

    /** {@code [ubyte]} or {@code [byte]}: the only types nested_flatbuffer and flexbuffer accept. */
    static boolean isByteVector(SchemaType type) {
        return type instanceof Vector v && v.element() instanceof Scalar s
                && (s.kind() == ScalarKind.UBYTE || s.kind() == ScalarKind.BYTE);
    }
}
