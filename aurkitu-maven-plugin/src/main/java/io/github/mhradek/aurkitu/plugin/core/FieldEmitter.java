package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Emits a single field line, including type overrides, fixed-array rewrite,
 * and attribute collection.
 */
@Slf4j
final class FieldEmitter {

    private final SchemaGenerator.Config config;
    private final TypeMapper typeMapper;
    private final List<String> validationIssues;

    FieldEmitter(SchemaGenerator.Config config, TypeMapper typeMapper, List<String> validationIssues) {
        this.config = config;
        this.typeMapper = typeMapper;
        this.validationIssues = validationIssues;
    }

    String buildField(FieldInfo field, ClassInfo owner, boolean isStruct, boolean emitIds) {
        String name = field.getName();
        String typeName = typeMapper.mapType(field.getTypeSignatureOrTypeDescriptor(), owner, field.getName(), field);
        boolean isArray = typeName != null && typeName.startsWith("[") && typeName.endsWith("]");

        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object ft = options.getParameterValues().get("fieldType").getValue();
            if (ft != null) {
                String ftStr = ft.toString();
                if (!ftStr.contains("MAP") && !ftStr.contains("ARRAY") && !ftStr.contains("IDENT")) {
                    String override = ftStr.toLowerCase().replace("fieldtype.", "");
                    if (override.contains(".")) {
                        override = override.substring(override.lastIndexOf('.') + 1).toLowerCase();
                    }
                    if (isArray && !override.contains("[")) {
                        typeName = "[" + override + "]";
                    } else {
                        typeName = override;
                    }
                }
                if (ftStr.contains("UBYTE") || ftStr.contains("USHORT") || ftStr.contains("UINT") || ftStr.contains("ULONG")) {
                    log.debug("Unsigned type {} on {}.{} (Java has no unsigned primitives)", ft, owner.getSimpleName(), name);
                }
            }
        }

        if (typeName == null) {
            log.debug("Skipping unsupported field: {}", name);
            if (config.validate) {
                validationIssues.add("Unsupported field type: " + owner.getSimpleName() + "." + name);
            }
            return null;
        }

        if (isStruct && field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object fl = options.getParameterValues().get("fixedLength").getValue();
            if (fl instanceof Number ln && ln.intValue() > 0) {
                int n = ln.intValue();
                String before = typeName;
                if (typeName.startsWith("[") && typeName.endsWith("]") && !typeName.contains(":")) {
                    typeName = typeName.substring(0, typeName.length() - 1) + ":" + n + "]";
                } else if (!typeName.contains("[")) {
                    typeName = "[" + typeName + ":" + n + "]";
                }
                log.debug("fixedLength rewrite on {}.{}: {} -> {}", owner.getSimpleName(), name, before, typeName);
            }
        }

        StringBuilder line = new StringBuilder();

        if (field.hasAnnotation(FlatBufferComment.class.getName())) {
            var commentAnno = field.getAnnotationInfo(FlatBufferComment.class.getName());
            String comment = String.valueOf(commentAnno.getParameterValues().get("comment").getValue());
            if (!comment.isBlank()) {
                line.append("// ").append(comment).append("\n  ");
            }
        }

        String safeName = name.replace('$', '_');
        if (config.snakeCaseFields) {
            safeName = Naming.toSnakeCase(safeName);
        }
        line.append(safeName).append(":").append(typeName);

        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            String defaultValue = Optional.ofNullable(options.getParameterValues().get("defaultValue"))
                    .map(p -> String.valueOf(p.getValue()))
                    .orElse("");
            if (!defaultValue.isBlank()) {
                line.append(" = ").append(defaultValue);
            }

            List<String> attrs = new ArrayList<>();
            if (Boolean.TRUE.equals(options.getParameterValues().get("required").getValue())) {
                attrs.add("required");
            }
            if (Boolean.TRUE.equals(options.getParameterValues().get("deprecated").getValue())) {
                attrs.add("deprecated");
            }
            if (emitIds) {
                Object fid = options.getParameterValues().get("fieldId").getValue();
                if (fid instanceof Number n && n.intValue() >= 0) {
                    attrs.add("id: " + n.intValue());
                }
            }
            if (Boolean.TRUE.equals(options.getParameterValues().get("key").getValue())) {
                attrs.add("key");
            }
            Object hash = options.getParameterValues().get("hash").getValue();
            if (hash != null && !String.valueOf(hash).isBlank()) {
                attrs.add("hash: \"" + hash + "\"");
            }
            Object nfb = options.getParameterValues().get("nestedFlatbuffer").getValue();
            if (nfb != null && !String.valueOf(nfb).isBlank()) {
                if ("[ubyte]".equals(typeName) || "[byte]".equals(typeName)) {
                    attrs.add("nested_flatbuffer: \"" + nfb + "\"");
                } else if (config.validate) {
                    validationIssues.add("nested_flatbuffer on " + owner.getSimpleName() + "." + name + " skipped: type must be [ubyte]");
                    log.warn("nested_flatbuffer on {}.{} skipped (type is {}, must be [ubyte])", owner.getSimpleName(), name, typeName);
                }
            }
            if (Boolean.TRUE.equals(options.getParameterValues().get("flexbuffer").getValue())) {
                if ("[ubyte]".equals(typeName) || "[byte]".equals(typeName)) {
                    attrs.add("flexbuffer");
                } else if (config.validate) {
                    validationIssues.add("flexbuffer on " + owner.getSimpleName() + "." + name + " skipped: type must be [ubyte]");
                    log.warn("flexbuffer on {}.{} skipped (type is {}, must be [ubyte])", owner.getSimpleName(), name, typeName);
                }
            }
            Object fa = options.getParameterValues().get("forceAlign").getValue();
            if (fa instanceof Number fn && fn.intValue() > 0) {
                attrs.add("force_align: " + fn.intValue());
            }
            if (!attrs.isEmpty()) {
                line.append(" (").append(String.join(", ", attrs)).append(")");
            }
        }

        return line.toString();
    }

    boolean hasFieldId(FieldInfo field) {
        if (!field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            return false;
        }
        var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
        Object fid = options.getParameterValues().get("fieldId").getValue();
        return fid instanceof Number n && n.intValue() >= 0;
    }
}
