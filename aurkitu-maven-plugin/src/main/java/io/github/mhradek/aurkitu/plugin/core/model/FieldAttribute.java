package io.github.mhradek.aurkitu.plugin.core.model;

/**
 * A field attribute, written inside the parentheses after the field's type.
 */
public sealed interface FieldAttribute {

    String write();

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
