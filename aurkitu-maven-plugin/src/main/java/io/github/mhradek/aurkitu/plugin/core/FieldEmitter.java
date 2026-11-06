package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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
            Object ft = Naming.param(options, "fieldType");
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
                    log.debug("Unsigned type {} on {}.{} (Java has no unsigned primitives)", ft, TypeRegistry.typeName(owner.getName()), name);
                }
            }
        }

        if (typeName == null) {
            log.debug("Skipping unsupported field: {}", name);
            if (config.validate) {
                validationIssues.add("Unsupported field type: " + TypeRegistry.typeName(owner.getName()) + "." + name);
            }
            return null;
        }

        if (isStruct && field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object fl = Naming.param(options, "fixedLength");
            if (fl instanceof Number ln && ln.intValue() > 0) {
                int n = ln.intValue();
                String before = typeName;
                if (typeName.startsWith("[") && typeName.endsWith("]") && !typeName.contains(":")) {
                    typeName = typeName.substring(0, typeName.length() - 1) + ":" + n + "]";
                } else if (!typeName.contains("[")) {
                    typeName = "[" + typeName + ":" + n + "]";
                }
                log.debug("fixedLength rewrite on {}.{}: {} -> {}", TypeRegistry.typeName(owner.getName()), name, before, typeName);
            }
        }

        StringBuilder line = new StringBuilder();

        if (field.hasAnnotation(FlatBufferComment.class.getName())) {
            var commentAnno = field.getAnnotationInfo(FlatBufferComment.class.getName());
            String comment = Naming.stringParam(commentAnno, "comment");
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
            String defaultValue = Naming.stringParam(options, "defaultValue");
            if (!defaultValue.isBlank()) {
                line.append(" = ").append(defaultValue);
            }

            List<String> attrs = new ArrayList<>();
            if (Boolean.TRUE.equals(Naming.param(options, "required"))) {
                attrs.add("required");
            }
            if (Boolean.TRUE.equals(Naming.param(options, "deprecated"))) {
                attrs.add("deprecated");
            }
            if (emitIds) {
                Object fid = Naming.param(options, "fieldId");
                if (fid instanceof Number n && n.intValue() >= 0) {
                    attrs.add("id: " + n.intValue());
                }
            }
            if (Boolean.TRUE.equals(Naming.param(options, "key"))) {
                attrs.add("key");
            }
            Object hash = Naming.param(options, "hash");
            if (hash != null && !String.valueOf(hash).isBlank()) {
                attrs.add("hash: \"" + hash + "\"");
            }
            Object nfb = Naming.param(options, "nestedFlatbuffer");
            if (nfb != null && !String.valueOf(nfb).isBlank()) {
                if ("[ubyte]".equals(typeName) || "[byte]".equals(typeName)) {
                    attrs.add("nested_flatbuffer: \"" + nfb + "\"");
                } else if (config.validate) {
                    validationIssues.add("nested_flatbuffer on " + TypeRegistry.typeName(owner.getName()) + "." + name + " skipped: type must be [ubyte]");
                    log.warn("nested_flatbuffer on {}.{} skipped (type is {}, must be [ubyte])", TypeRegistry.typeName(owner.getName()), name, typeName);
                }
            }
            if (Boolean.TRUE.equals(Naming.param(options, "flexbuffer"))) {
                if ("[ubyte]".equals(typeName) || "[byte]".equals(typeName)) {
                    attrs.add("flexbuffer");
                } else if (config.validate) {
                    validationIssues.add("flexbuffer on " + TypeRegistry.typeName(owner.getName()) + "." + name + " skipped: type must be [ubyte]");
                    log.warn("flexbuffer on {}.{} skipped (type is {}, must be [ubyte])", TypeRegistry.typeName(owner.getName()), name, typeName);
                }
            }
            Object fa = Naming.param(options, "forceAlign");
            if (fa instanceof Number fn && fn.intValue() > 0) {
                if (typeName.startsWith("[")) {
                    attrs.add("force_align: " + fn.intValue());
                } else if (config.validate) {
                    validationIssues.add("force_align on " + TypeRegistry.typeName(owner.getName()) + "." + name + " skipped: only valid on vectors");
                    log.warn("force_align on {}.{} skipped (type is {}, only valid on vectors)", TypeRegistry.typeName(owner.getName()), name, typeName);
                }
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
        Object fid = Naming.param(options, "fieldId");
        return fid instanceof Number n && n.intValue() >= 0;
    }
}
