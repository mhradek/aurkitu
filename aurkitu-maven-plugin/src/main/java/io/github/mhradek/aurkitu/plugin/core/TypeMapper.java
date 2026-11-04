package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.*;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;

import java.util.List;
import java.util.Map;

/**
 * Maps ClassGraph type signatures to FlatBuffers type names.
 * Handles arrays, lists, maps (via generated MapValueSet tables), wrappers,
 * common types, and primitives.
 */
final class TypeMapper {

    private final SchemaGenerator.Config config;
    private final Map<String, String> generatedMapTables;

    TypeMapper(SchemaGenerator.Config config, Map<String, String> generatedMapTables) {
        this.config = config;
        this.generatedMapTables = generatedMapTables;
    }

    String mapType(TypeSignature typeSig, ClassInfo owner, String contextName, FieldInfo field) {
        if (typeSig == null) {
            return "string";
        }

        boolean useFullName = false;
        if (field != null && field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object ufn = options.getParameterValues().get("useFullName").getValue();
            if (Boolean.TRUE.equals(ufn)) {
                useFullName = true;
            }
        }

        if (typeSig instanceof ArrayTypeSignature) {
            ArrayTypeSignature arr = (ArrayTypeSignature) typeSig;
            String element = mapType(arr.getElementTypeSignature(), owner, contextName, field);
            return element == null ? null : "[" + element + "]";
        }

        if (typeSig instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature classRef = (ClassRefTypeSignature) typeSig;
            String className = classRef.getFullyQualifiedClassName();

            if ("java.util.Map".equals(className) || className.endsWith(".Map")) {
                List<TypeArgument> typeArgs = classRef.getTypeArguments();
                if (typeArgs.size() == 2) {
                    TypeSignature valueSig = typeArgs.get(1).getTypeSignature();
                    String valueType = mapType(valueSig, owner, contextName, field);
                    if (valueType != null) {
                        String mapTableName = "MapValueSet_" + owner.getSimpleName() + "_" + Naming.toSnakeCase(contextName);
                        String mapTable = "table " + mapTableName + " {\n  key:string;\n  value:" + valueType + ";\n}\n\n";
                        generatedMapTables.put(mapTableName, mapTable);
                        return "[" + mapTableName + "]";
                    }
                }
                return null;
            }

            if (className.endsWith("List") || className.endsWith("Set") || className.endsWith("Collection")) {
                List<TypeArgument> typeArgs = classRef.getTypeArguments();
                if (!typeArgs.isEmpty()) {
                    TypeSignature elementSig = typeArgs.get(0).getTypeSignature();
                    String element = mapType(elementSig, owner, contextName, field);
                    return element == null ? null : "[" + element + "]";
                }
                return null;
            }

            String wrapper = mapWrapper(className);
            if (wrapper != null) {
                return wrapper;
            }

            if (config.mapCommonTypes) {
                if ("java.lang.String".equals(className)) return "string";
                if ("java.util.Date".equals(className)) return "string";
                if ("java.net.URL".equals(className)) return "string";
                if ("java.math.BigDecimal".equals(className)) return "string";
            }

            if (className.startsWith("java.")) {
                return null;
            }
            String simple = className.substring(className.lastIndexOf('.') + 1);
            return useFullName ? className.replace('$', '_') : simple;
        }

        String sig = typeSig.toString();
        return switch (sig) {
            case "boolean" -> "bool";
            case "byte" -> "byte";
            case "short" -> "short";
            case "int" -> "int";
            case "long" -> "long";
            case "float" -> "float";
            case "double" -> "double";
            default -> null;
        };
    }

    private static String mapWrapper(String className) {
        return switch (className) {
            case "java.lang.Boolean" -> "bool";
            case "java.lang.Byte" -> "byte";
            case "java.lang.Short" -> "short";
            case "java.lang.Integer" -> "int";
            case "java.lang.Long" -> "long";
            case "java.lang.Float" -> "float";
            case "java.lang.Double" -> "double";
            default -> null;
        };
    }
}
