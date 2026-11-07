package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.AnnotationParameterValue;

/**
 * Shared naming helpers for schema emission.
 */
final class Naming {

    private Naming() {}

    static String toSnakeCase(String name) {
        return toSnakeCase(name, false);
    }

    /**
     * camelCase to snake_case. Without {@code acronyms}, every capital starts a word ({@code userID} becomes
     * {@code user_i_d}, as in 1.0); with it, a run of capitals is one word ({@code userID} becomes {@code user_id},
     * {@code XMLParser} becomes {@code xml_parser}).
     */
    static String toSnakeCase(String name, boolean acronyms) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && sb.charAt(sb.length() - 1) != '_' && startsWord(name, i, acronyms)) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Whether the capital at {@code i} starts a new word. */
    private static boolean startsWord(String name, int i, boolean acronyms) {
        if (!acronyms || !Character.isUpperCase(name.charAt(i - 1))) {
            return true;
        }
        // Inside a run of capitals, only the last one starts a word, when a lowercase letter follows (XMLParser),
        // unless that is a plural s ending the acronym (userIDs, URLsByHost).
        if (i + 1 >= name.length() || !Character.isLowerCase(name.charAt(i + 1))) {
            return false;
        }
        boolean pluralS = name.charAt(i + 1) == 's'
                && (i + 2 == name.length() || !Character.isLowerCase(name.charAt(i + 2)));
        return !pluralS;
    }

    /** Schema name of a type: the simple Java name, without any outer class. */
    static String typeName(String className) {
        String simple = className.substring(className.lastIndexOf('.') + 1);
        return simple.substring(simple.lastIndexOf('$') + 1);
    }

    /** Schema name of a field: '$' (not valid in schema identifiers) becomes '_', then optional snake_case. */
    static String fieldName(String javaName, boolean snakeCase) {
        return fieldName(javaName, snakeCase, false);
    }

    static String fieldName(String javaName, boolean snakeCase, boolean acronyms) {
        String name = javaName.replace('$', '_');
        return snakeCase ? toSnakeCase(name, acronyms) : name;
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
