package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferRpcService;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns every annotated class its schema {@link TypeName}, so declarations and
 * references always agree. Types are named by their simple Java name (nested
 * {@code Outer.Inner} becomes {@code Inner}) in their annotation's namespace,
 * or the default namespace.
 *
 * <p>Classes and enums nested directly in an annotated table or struct are declared too, in
 * the outer type's namespace, without needing their own annotation (as in 0.0.x). Annotate a
 * nested type with {@code @FlatBufferIgnore} to leave it out.</p>
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
        registry.register(scanResult.getClassesWithAnnotation(FlatBufferRpcService.class.getName()), FlatBufferRpcService.class.getName());
        for (ClassInfo outer : scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName())) {
            for (ClassInfo nested : nestedTypes(outer)) {
                registry.names.putIfAbsent(nested.getName(),
                        new TypeName(registry.nameOf(outer.getName()).namespace(), Naming.typeName(nested.getName())));
            }
        }
        return registry;
    }

    /**
     * Unannotated classes and enums declared directly inside {@code outer} that become schema types:
     * not synthetic, anonymous, local, interfaces or annotations, and not marked {@code @FlatBufferIgnore}.
     */
    static List<ClassInfo> nestedTypes(ClassInfo outer) {
        List<ClassInfo> nested = new ArrayList<>();
        for (ClassInfo inner : outer.getInnerClasses()) {
            boolean direct = inner.getName().startsWith(outer.getName() + "$")
                    && inner.getName().indexOf('$', outer.getName().length() + 1) < 0;
            boolean annotated = inner.hasAnnotation(FlatBufferTable.class.getName())
                    || inner.hasAnnotation(FlatBufferEnum.class.getName());
            if (direct && !annotated && !inner.isSynthetic() && !inner.isAnonymousInnerClass()
                    && !inner.isInterfaceOrAnnotation() && !inner.hasAnnotation(FlatBufferIgnore.class.getName())) {
                nested.add(inner);
            }
        }
        return nested;
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

    /**
     * The declared type named {@code simpleName}: the one in {@code preferredNamespace} if there is one,
     * else the only one; otherwise (none or ambiguous) that name in {@code preferredNamespace}.
     */
    TypeName resolve(String simpleName, String preferredNamespace) {
        List<TypeName> matches = names.values().stream().filter(n -> n.name().equals(simpleName)).distinct().toList();
        return matches.stream().filter(n -> n.namespace().equals(preferredNamespace)).findFirst()
                .orElse(matches.size() == 1 ? matches.get(0) : new TypeName(preferredNamespace, simpleName));
    }

    /** The schema name of a class; classes that are not annotated are assumed to be in the default namespace. */
    TypeName nameOf(String className) {
        return names.getOrDefault(className, new TypeName(defaultNamespace, Naming.typeName(className)));
    }
}
