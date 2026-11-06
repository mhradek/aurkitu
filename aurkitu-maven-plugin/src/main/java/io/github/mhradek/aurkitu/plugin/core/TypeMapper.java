package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeSignature;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps ClassGraph type signatures to FlatBuffers type names.
 * Handles arrays, collections, maps (via generated MapValueSet tables), wrappers,
 * common types, primitives and references to other declared types.
 */
final class TypeMapper {

    /** Prefix of the tables generated for {@code Map} fields. */
    static final String MAP_TABLE_PREFIX = "MapValueSet_";

    /** JDK value types emitted as {@code string} when {@code mapCommonTypes} is enabled. */
    static final Set<String> COMMON_TYPES = Set.of(
            "java.util.Date",
            "java.util.UUID",
            "java.net.URL",
            "java.net.URI",
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.time.Instant",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.OffsetDateTime",
            "java.time.ZonedDateTime",
            "java.time.Duration");

    private final SchemaGenerator.Config config;
    private final Map<String, String> generatedMapTables;
    private final TypeRegistry registry;

    TypeMapper(SchemaGenerator.Config config, Map<String, String> generatedMapTables, TypeRegistry registry) {
        this.config = config;
        this.generatedMapTables = generatedMapTables;
        this.registry = registry;
    }

    /**
     * @return the FlatBuffers type for a field, or null if it cannot be represented
     */
    String mapType(TypeSignature typeSig, ClassInfo owner, String contextName, FieldInfo field) {
        boolean useFullName = field != null
                && Boolean.TRUE.equals(Naming.param(field.getAnnotationInfo(FlatBufferFieldOptions.class.getName()), "useFullName"));
        return mapType(typeSig, owner, contextName, useFullName, registry.namespaceOf(owner.getName()));
    }

    private String mapType(TypeSignature typeSig, ClassInfo owner, String contextName, boolean useFullName, String scope) {
        if (typeSig == null) {
            return null;
        }

        if (typeSig instanceof ArrayTypeSignature arr) {
            if (arr.getNumDimensions() > 1) {
                return null; // FlatBuffers has no nested vectors.
            }
            TypeSignature elementSig = arr.getElementTypeSignature();
            // byte[] is binary data; [ubyte] is the FlatBuffers convention (and what nested_flatbuffer/flexbuffer require).
            if (elementSig instanceof BaseTypeSignature base && "byte".equals(base.getTypeStr())) {
                return "[ubyte]";
            }
            String element = mapType(elementSig, owner, contextName, useFullName, scope);
            // FlatBuffers has no nested vectors.
            return element == null || element.startsWith("[") ? null : "[" + element + "]";
        }

        if (typeSig instanceof ClassRefTypeSignature classRef) {
            return mapClassRef(classRef, owner, contextName, useFullName, scope);
        }

        if (typeSig instanceof BaseTypeSignature base) {
            return switch (base.getTypeStr()) {
                case "boolean" -> "bool";
                case "byte" -> "byte";
                case "short" -> "short";
                case "int" -> "int";
                case "long" -> "long";
                case "float" -> "float";
                case "double" -> "double";
                default -> null; // char, void
            };
        }

        return null; // type variables and wildcards
    }

    private String mapClassRef(ClassRefTypeSignature classRef, ClassInfo owner, String contextName,
                               boolean useFullName, String scope) {
        String className = classRef.getFullyQualifiedClassName();
        List<TypeArgument> typeArgs = classRef.getTypeArguments();

        if (isJdkSubtype(className, Map.class)) {
            if (typeArgs.size() != 2) {
                return null;
            }
            // Map tables are emitted at the end of the schema, in the default namespace.
            String valueType = mapType(typeArgs.get(1).getTypeSignature(), owner, contextName, useFullName, registry.defaultNamespace());
            if (valueType == null) {
                return null;
            }
            String mapTableName = MAP_TABLE_PREFIX + TypeRegistry.typeName(owner.getName()) + "_" + Naming.toSnakeCase(contextName);
            generatedMapTables.put(mapTableName, "table " + mapTableName + " {\n  key:string;\n  value:" + valueType + ";\n}\n\n");
            String ref = "[" + mapTableName + "]";
            return scope.equals(registry.defaultNamespace()) ? ref : "[" + registry.defaultNamespace() + "." + mapTableName + "]";
        }

        if (isJdkSubtype(className, Collection.class)) {
            if (typeArgs.size() != 1) {
                return null;
            }
            String element = mapType(typeArgs.get(0).getTypeSignature(), owner, contextName, useFullName, scope);
            return element == null || element.startsWith("[") ? null : "[" + element + "]";
        }

        String scalar = switch (className) {
            case "java.lang.String" -> "string";
            case "java.lang.Boolean" -> "bool";
            case "java.lang.Byte" -> "byte";
            case "java.lang.Short" -> "short";
            case "java.lang.Integer" -> "int";
            case "java.lang.Long" -> "long";
            case "java.lang.Float" -> "float";
            case "java.lang.Double" -> "double";
            default -> null;
        };
        if (scalar != null) {
            return scalar;
        }

        if (config.mapCommonTypes && COMMON_TYPES.contains(className)) {
            return "string";
        }

        if (className.startsWith("java.") || className.startsWith("javax.")) {
            return null;
        }
        if (useFullName) {
            return className.replace('$', '_');
        }
        return registry.reference(className, scope);
    }

    /** True if {@code className} is a JDK type assignable to {@code parent} (e.g. ArrayList to Collection). */
    static boolean isJdkSubtype(String className, Class<?> parent) {
        if (!className.startsWith("java.")) {
            return false;
        }
        try {
            return parent.isAssignableFrom(Class.forName(className, false, ClassLoader.getPlatformClassLoader()));
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
