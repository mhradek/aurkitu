package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Emits a table or struct, including inheritance walk and struct constraints.
 */
@Slf4j
final class TableEmitter {

    private final SchemaGenerator.Config config;
    private final FieldEmitter fieldEmitter;
    private final List<String> validationIssues;

    TableEmitter(SchemaGenerator.Config config, FieldEmitter fieldEmitter, List<String> validationIssues) {
        this.config = config;
        this.fieldEmitter = fieldEmitter;
        this.validationIssues = validationIssues;
    }

    String buildTable(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        boolean isStruct = isStruct(classInfo);

        String name = TypeRegistry.typeName(classInfo.getName());

        String comment = Naming.stringParam(classInfo.getAnnotationInfo(FlatBufferComment.class.getName()), "comment");
        if (!comment.isBlank()) {
            sb.append("// ").append(comment).append("\n");
        }

        sb.append(isStruct ? "struct " : "table ").append(name).append(" {\n");

        List<FieldInfo> fields = collectFields(classInfo);

        long withId = fields.stream().filter(fieldEmitter::hasFieldId).count();
        boolean emitIds = withId == 0 || withId == fields.size();
        if (withId > 0 && withId < fields.size() && config.validate) {
            validationIssues.add("Partial field ids on " + name + "; ids omitted (all fields or none must have an id)");
            log.warn("Partial field ids on {}; omitting id attributes", name);
        }

        for (FieldInfo field : fields) {
            String fieldLine = fieldEmitter.buildField(field, classInfo, isStruct, emitIds);
            if (fieldLine != null) {
                if (isStruct && isNonScalarField(fieldLine)) {
                    if (config.validate) {
                        validationIssues.add("Struct " + name + " field " + field.getName() + " is non-scalar; structs may only contain scalars or other structs");
                    }
                    if (config.failOnValidationIssues) {
                        continue;
                    }
                }
                sb.append("  ").append(fieldLine).append(";\n");
            }
        }

        sb.append("}\n\n");
        return sb.toString();
    }

    private List<FieldInfo> collectFields(ClassInfo classInfo) {
        List<FieldInfo> fields = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        ClassInfo current = classInfo;
        while (current != null) {
            for (FieldInfo field : current.getDeclaredFieldInfo()) {
                String fname = field.getName();
                if (seen.contains(fname)) {
                    continue;
                }
                seen.add(fname);

                // Compiler-generated fields, e.g. this$0 on non-static inner classes.
                if (field.isSynthetic() || field.hasAnnotation(FlatBufferIgnore.class.getName())) {
                    continue;
                }
                if (config.ignoreStaticMembers && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
            if (current != null && "java.lang.Object".equals(current.getName())) {
                break;
            }
        }
        return fields;
    }

    static boolean isStruct(ClassInfo classInfo) {
        var tableAnno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        if (tableAnno == null) {
            return false;
        }
        return "STRUCT".equals(Naming.enumConstantName(Naming.param(tableAnno, "value")));
    }

    static boolean isRootType(ClassInfo classInfo) {
        var anno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        return anno != null && Boolean.TRUE.equals(Naming.param(anno, "rootType"));
    }

    private static boolean isNonScalarField(String fieldLine) {
        int colon = fieldLine.indexOf(':');
        if (colon < 0) return false;
        String type = fieldLine.substring(colon + 1);
        int space = type.indexOf(' ');
        if (space > 0) type = type.substring(0, space);
        int paren = type.indexOf('(');
        if (paren > 0) type = type.substring(0, paren);
        if (type.matches("\\[[a-z]+:\\d+\\]")) {
            return false;
        }
        return type.contains("[") || type.contains("string") || type.contains("table");
    }
}
