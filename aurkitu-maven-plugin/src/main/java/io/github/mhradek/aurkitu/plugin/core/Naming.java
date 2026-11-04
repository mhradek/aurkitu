package io.github.mhradek.aurkitu.plugin.core;

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

    /** Extract the constant name from a ClassGraph enum value ("...Type.STRUCT" -> "STRUCT"). */
    static String enumConstantName(Object raw) {
        if (raw == null) return "";
        String s = String.valueOf(raw);
        int dot = s.lastIndexOf('.');
        return (dot >= 0 ? s.substring(dot + 1) : s).toUpperCase();
    }
}
