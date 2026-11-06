package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ArrayTypeSignature;
import io.github.classgraph.BaseTypeSignature;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassRefTypeSignature;
import io.github.classgraph.TypeArgument;
import io.github.classgraph.TypeSignature;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.StringType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import io.github.mhradek.aurkitu.plugin.core.model.TableDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps Java field types (ClassGraph type signatures) to schema types.
 * Handles arrays, collections, maps (via generated MapValueSet tables), wrappers,
 * common types, primitives and references to other declared types.
 */
final class TypeMapper {

    /** Prefix of the tables generated for {@code Map} fields. */
    static final String MAP_TABLE_PREFIX = "MapValueSet_";

    /** JDK value types mapped to {@code string} when {@code mapCommonTypes} is enabled. */
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

    private static final Map<String, ScalarKind> PRIMITIVES = Map.of(
            "boolean", ScalarKind.BOOL,
            "byte", ScalarKind.BYTE,
            "short", ScalarKind.SHORT,
            "int", ScalarKind.INT,
            "long", ScalarKind.LONG,
            "float", ScalarKind.FLOAT,
            "double", ScalarKind.DOUBLE);

    private static final Map<String, ScalarKind> WRAPPERS = Map.of(
            "java.lang.Boolean", ScalarKind.BOOL,
            "java.lang.Byte", ScalarKind.BYTE,
            "java.lang.Short", ScalarKind.SHORT,
            "java.lang.Integer", ScalarKind.INT,
            "java.lang.Long", ScalarKind.LONG,
            "java.lang.Float", ScalarKind.FLOAT,
            "java.lang.Double", ScalarKind.DOUBLE);

    private final boolean mapCommonTypes;
    private final TypeRegistry registry;
    private final Map<String, TableDeclaration> mapTables;

    /**
     * @param mapTables receives a generated table for every {@code Map} field, keyed by name
     */
    TypeMapper(boolean mapCommonTypes, TypeRegistry registry, Map<String, TableDeclaration> mapTables) {
        this.mapCommonTypes = mapCommonTypes;
        this.registry = registry;
        this.mapTables = mapTables;
    }

    /**
     * @param owner       the class declaring the field (names generated map tables)
     * @param fieldName   the Java field name (names generated map tables)
     * @param useFullName reference user types by their fully qualified Java name
     * @return the schema type, or null if the Java type has no schema equivalent
     */
    SchemaType map(TypeSignature typeSig, ClassInfo owner, String fieldName, boolean useFullName) {
        if (typeSig instanceof BaseTypeSignature base) {
            ScalarKind kind = PRIMITIVES.get(base.getTypeStr());
            return kind == null ? null : new Scalar(kind); // char and void have no equivalent
        }
        if (typeSig instanceof ArrayTypeSignature arr) {
            if (arr.getNumDimensions() > 1) {
                return null; // FlatBuffers has no nested vectors.
            }
            TypeSignature elementSig = arr.getElementTypeSignature();
            // byte[] is binary data; [ubyte] is the FlatBuffers convention (and what nested_flatbuffer/flexbuffer require).
            if (elementSig instanceof BaseTypeSignature base && "byte".equals(base.getTypeStr())) {
                return new Vector(new Scalar(ScalarKind.UBYTE));
            }
            return vectorOf(map(elementSig, owner, fieldName, useFullName));
        }
        if (typeSig instanceof ClassRefTypeSignature classRef) {
            return mapClassRef(classRef, owner, fieldName, useFullName);
        }
        return null; // type variables and wildcards
    }

    private SchemaType mapClassRef(ClassRefTypeSignature classRef, ClassInfo owner, String fieldName, boolean useFullName) {
        String className = classRef.getFullyQualifiedClassName();
        List<TypeArgument> typeArgs = classRef.getTypeArguments();
        if (isJdkSubtype(className, Map.class)) {
            return typeArgs.size() == 2 ? mapTable(typeArgs.get(1).getTypeSignature(), owner, fieldName, useFullName) : null;
        }
        if (isJdkSubtype(className, Collection.class)) {
            return typeArgs.size() == 1 ? vectorOf(map(typeArgs.get(0).getTypeSignature(), owner, fieldName, useFullName)) : null;
        }
        SchemaType value = valueType(className);
        if (value != null || className.startsWith("java.") || className.startsWith("javax.")) {
            return value; // other JDK types have no schema equivalent
        }
        return useFullName ? fullNameReference(className) : new Reference(registry.nameOf(className));
    }

    /**
     * A {@code Map} becomes a vector of a generated {@code key:string; value:T} table,
     * written at the end of the schema in the default namespace.
     */
    private SchemaType mapTable(TypeSignature valueSig, ClassInfo owner, String fieldName, boolean useFullName) {
        SchemaType valueType = map(valueSig, owner, fieldName, useFullName);
        if (valueType == null) {
            return null;
        }
        TypeName mapTable = new TypeName(registry.defaultNamespace(),
                MAP_TABLE_PREFIX + Naming.typeName(owner.getName()) + "_" + Naming.toSnakeCase(fieldName));
        mapTables.put(mapTable.name(), new TableDeclaration(mapTable, owner.getName(), "",
                List.of(Field.of("key", StringType.INSTANCE), Field.of("value", valueType)), false));
        return new Vector(new Reference(mapTable));
    }

    /** String, boxed scalars and (optionally) common JDK value types; null for anything else. */
    private SchemaType valueType(String className) {
        if ("java.lang.String".equals(className) || mapCommonTypes && COMMON_TYPES.contains(className)) {
            return StringType.INSTANCE;
        }
        ScalarKind wrapper = WRAPPERS.get(className);
        return wrapper == null ? null : new Scalar(wrapper);
    }

    /** {@code useFullName}: the Java package as namespace, always written qualified. */
    private static Reference fullNameReference(String className) {
        int dot = className.lastIndexOf('.');
        return new Reference(new TypeName(dot < 0 ? "" : className.substring(0, dot),
                className.substring(dot + 1).replace('$', '_')), true);
    }

    /** {@code [element]}; null when the element is unsupported or itself a vector (FlatBuffers has no nested vectors). */
    private static SchemaType vectorOf(SchemaType element) {
        return element == null || element instanceof Vector ? null : new Vector(element);
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
