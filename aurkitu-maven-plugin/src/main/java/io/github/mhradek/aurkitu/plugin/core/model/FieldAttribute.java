package io.github.mhradek.aurkitu.plugin.core.model;

/**
 * An attribute, written in the parentheses after a field, or after the name of a table, struct,
 * enum or union.
 */
public sealed interface FieldAttribute {

    String write();

    /** Tables: keep fields in declaration order in the buffer. */
    record OriginalOrder() implements FieldAttribute {
        @Override
        public String write() {
            return "original_order";
        }
    }

    /** Enums: values are bit positions. */
    record BitFlags() implements FieldAttribute {
        @Override
        public String write() {
            return "bit_flags";
        }
    }

    /**
     * A user-defined attribute, declared in the schema with {@code attribute "name";}.
     *
     * @param value the value as written after the colon (a number or a quoted string), or empty for none
     */
    record Custom(String name, String value) implements FieldAttribute {
        @Override
        public String write() {
            return value.isEmpty() ? name : name + ": " + value;
        }
    }

    record Required() implements FieldAttribute {
        @Override
        public String write() {
            return "required";
        }
    }

    record Deprecated() implements FieldAttribute {
        @Override
        public String write() {
            return "deprecated";
        }
    }

    record Id(int value) implements FieldAttribute {
        @Override
        public String write() {
            return "id: " + value;
        }
    }

    record Key() implements FieldAttribute {
        @Override
        public String write() {
            return "key";
        }
    }

    record Hash(String algorithm) implements FieldAttribute {
        @Override
        public String write() {
            return "hash: \"" + algorithm + "\"";
        }
    }

    record NestedFlatbuffer(String rootType) implements FieldAttribute {
        @Override
        public String write() {
            return "nested_flatbuffer: \"" + rootType + "\"";
        }
    }

    record Flexbuffer() implements FieldAttribute {
        @Override
        public String write() {
            return "flexbuffer";
        }
    }

    record ForceAlign(int alignment) implements FieldAttribute {
        @Override
        public String write() {
            return "force_align: " + alignment;
        }
    }
}
