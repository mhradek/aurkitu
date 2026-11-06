package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for schema type names and namespaces, so that
 * declarations, field references and {@code root_type} always agree.
 *
 * <p>Types are declared by their simple Java name (nested classes drop the
 * outer class, e.g. {@code Outer.Inner} becomes {@code Inner}). References
 * from a different namespace are fully qualified.</p>
 */
final class TypeRegistry {

    private final String defaultNamespace;
    /** Fully-qualified Java class name to schema namespace, for every declared type. */
    private final Map<String, String> namespaces = new HashMap<>();

    TypeRegistry(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    static TypeRegistry build(ScanResult scanResult, String defaultNamespace, boolean validate, List<String> validationIssues) {
        TypeRegistry registry = new TypeRegistry(defaultNamespace);
        Map<String, String> declaredBy = new HashMap<>();
        register(registry, scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName()), FlatBufferEnum.class.getName(), declaredBy, validate, validationIssues);
        register(registry, scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName()), FlatBufferTable.class.getName(), declaredBy, validate, validationIssues);
        return registry;
    }

    private static void register(TypeRegistry registry, List<ClassInfo> classes, String annotation,
                                 Map<String, String> declaredBy, boolean validate, List<String> validationIssues) {
        for (ClassInfo classInfo : classes) {
            String ns = Naming.stringParam(classInfo.getAnnotationInfo(annotation), "namespace");
            if (ns.isBlank()) {
                ns = registry.defaultNamespace;
            }
            registry.namespaces.put(classInfo.getName(), ns);

            String qualified = ns + "." + typeName(classInfo.getName());
            String previous = declaredBy.putIfAbsent(qualified, classInfo.getName());
            if (previous != null && validate) {
                validationIssues.add("Duplicate type name " + qualified + " (declared by " + previous + " and " + classInfo.getName() + ")");
            }
        }
    }

    String defaultNamespace() {
        return defaultNamespace;
    }

    /** Namespace a type is declared in; undeclared types are assumed to live in the default namespace. */
    String namespaceOf(String className) {
        return namespaces.getOrDefault(className, defaultNamespace);
    }

    /** Schema name used to declare a type: the simple name, without any outer class. */
    static String typeName(String className) {
        String simple = className.substring(className.lastIndexOf('.') + 1);
        return simple.substring(simple.lastIndexOf('$') + 1);
    }

    /** Name used to reference {@code className} from code emitted in {@code scopeNamespace}. */
    String reference(String className, String scopeNamespace) {
        String name = typeName(className);
        String ns = namespaceOf(className);
        return ns.equals(scopeNamespace) ? name : ns + "." + name;
    }
}
