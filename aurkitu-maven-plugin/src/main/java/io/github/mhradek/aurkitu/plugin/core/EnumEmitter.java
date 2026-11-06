package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/**
 * Emits enums and unions.
 */
final class EnumEmitter {

    private EnumEmitter() {}

    static String buildEnum(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        var enumAnno = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        boolean isUnion = false;
        String underlying = "byte";
        if (enumAnno != null) {
            Object val = Naming.param(enumAnno, "value");
            if ("UNION".equals(Naming.enumConstantName(val))) {
                isUnion = true;
            }
            Object et = Naming.param(enumAnno, "enumType");
            if (et != null) {
                String etStr = String.valueOf(et);
                if (etStr.contains("SHORT")) underlying = "short";
                else if (etStr.contains("INT")) underlying = "int";
                else if (etStr.contains("LONG")) underlying = "long";
            }
        }

        if (classInfo.hasAnnotation(FlatBufferComment.class.getName())) {
            var commentAnno = classInfo.getAnnotationInfo(FlatBufferComment.class.getName());
            String comment = Naming.stringParam(commentAnno, "comment");
            if (!comment.isBlank()) {
                sb.append("// ").append(comment).append("\n");
            }
        }

        if (isUnion) {
            sb.append("union ").append(classInfo.getSimpleName()).append(" {\n");
        } else {
            sb.append("enum ").append(classInfo.getSimpleName()).append(" : ").append(underlying).append(" {\n");
        }

        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.isEnum()) {
                sb.append("  ").append(field.getName());
                if (!isUnion) {
                    Object constant = field.getConstantInitializerValue();
                    if (constant != null) {
                        sb.append(" = ").append(constant);
                    }
                }
                sb.append(",\n");
            }
        }

        if (sb.toString().endsWith("{\n")) {
            // Load through ClassGraph so the scanned classpath (not the plugin's) is used.
            Class<?> enumClass = classInfo.loadClass(true);
            if (enumClass != null && enumClass.isEnum()) {
                for (Object c : enumClass.getEnumConstants()) {
                    sb.append("  ").append(((Enum<?>) c).name()).append(",\n");
                }
            }
        }

        sb.append("}\n\n");
        return sb.toString();
    }
}
