package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;

/**
 * Shared naming helpers for schema emission.
 */
final class Naming {

    private Naming() {}

    static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Schema name of a type: the simple Java name, without any outer class. */
    static String typeName(String className) {
        String simple = className.substring(className.lastIndexOf('.') + 1);
        return simple.substring(simple.lastIndexOf('$') + 1);
    }

    /** Schema name of a field: '$' (not valid in schema identifiers) becomes '_', then optional snake_case. */
    static String fieldName(String javaName, boolean snakeCase) {
        String name = javaName.replace('$', '_');
        return snakeCase ? toSnakeCase(name) : name;
    }

    /** Extract the constant name from a ClassGraph enum value ("...Type.STRUCT" -> "STRUCT"). */
    static String enumConstantName(Object raw) {
        if (raw == null) return "";
        String s = String.valueOf(raw);
        int dot = s.lastIndexOf('.');
        return (dot >= 0 ? s.substring(dot + 1) : s).toUpperCase();
    }

    /**
     * Null-safe annotation parameter lookup. ClassGraph only fills in default values when
     * the annotation class itself is on the scanned classpath, so an omitted parameter can
     * be absent. Returns null for a missing annotation or parameter.
     */
    static Object param(AnnotationInfo anno, String name) {
        if (anno == null) return null;
        AnnotationParameterValue value = anno.getParameterValues().get(name);
        return value == null ? null : value.getValue();
    }

    /** Like {@link #param} but returns "" instead of null. */
    static String stringParam(AnnotationInfo anno, String name) {
        Object value = param(anno, name);
        return value == null ? "" : String.valueOf(value);
    }
}
