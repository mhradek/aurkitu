package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns every annotated class its schema {@link TypeName}, so declarations and
 * references always agree. Types are named by their simple Java name (nested
 * {@code Outer.Inner} becomes {@code Inner}) in their annotation's namespace,
 * or the default namespace.
 */
final class TypeRegistry {

    private final String defaultNamespace;
    private final Map<String, TypeName> names = new HashMap<>();

    private TypeRegistry(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    static TypeRegistry build(ScanResult scanResult, String defaultNamespace) {
        TypeRegistry registry = new TypeRegistry(defaultNamespace);
        registry.register(scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName()), FlatBufferEnum.class.getName());
        registry.register(scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName()), FlatBufferTable.class.getName());
        return registry;
    }

    private void register(List<ClassInfo> classes, String annotation) {
        for (ClassInfo classInfo : classes) {
            String ns = Naming.stringParam(classInfo.getAnnotationInfo(annotation), "namespace");
            names.put(classInfo.getName(), new TypeName(ns.isBlank() ? defaultNamespace : ns, Naming.typeName(classInfo.getName())));
        }
    }

    String defaultNamespace() {
        return defaultNamespace;
    }

    /** The schema name of a class; classes that are not annotated are assumed to be in the default namespace. */
    TypeName nameOf(String className) {
        return names.getOrDefault(className, new TypeName(defaultNamespace, Naming.typeName(className)));
    }
}
