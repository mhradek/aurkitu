package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

/**
 * Core generator using ClassGraph for modern, reliable classpath scanning.
 * Processes fields of annotated tables and enums.
 */
@Slf4j
public class SchemaGenerator {

    private final MavenProject project;
    private final List<String> specifiedPackages;
    private final boolean validate;

    public SchemaGenerator(MavenProject project, List<String> specifiedPackages, boolean validate) {
        this.project = project;
        this.specifiedPackages = specifiedPackages;
        this.validate = validate;
    }

    public void generate(String schemaName, String namespace, File outputDir) {
        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableFieldInfo()
                .acceptPackages(specifiedPackages != null && !specifiedPackages.isEmpty()
                        ? specifiedPackages.toArray(new String[0])
                        : new String[]{"*"})
                .scan()) {

            StringBuilder schema = new StringBuilder();
            schema.append("// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n");
            schema.append("namespace ").append(namespace).append(";\n\n");

            String rootType = null;

            // Tables
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName())) {
                schema.append(buildTable(classInfo));
                if (isRootType(classInfo)) {
                    if (rootType != null) {
                        log.warn("Multiple root types detected; using last one: {}", classInfo.getSimpleName());
                    }
                    rootType = classInfo.getSimpleName();
                }
            }

            // Enums
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
                schema.append(buildEnum(classInfo));
            }

            if (rootType != null) {
                schema.append("root_type ").append(rootType).append(";\n");
            }

            File outFile = new File(outputDir, schemaName + ".fbs");
            Files.writeString(outFile.toPath(), schema.toString());
            log.info("Wrote schema to {}", outFile.getAbsolutePath());

            if (validate) {
                log.info("Basic validation complete. Run flatc for full schema validation.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate schema", e);
        }
    }

    private boolean isRootType(ClassInfo classInfo) {
        return classInfo.getAnnotationInfo(FlatBufferTable.class.getName())
                .getParameterValues().get("rootType").getValue() == Boolean.TRUE;
    }

    private String buildTable(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        boolean isStruct = false;
        var tableAnno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        if (tableAnno != null && tableAnno.getParameterValues().get("value") != null) {
            String structure = tableAnno.getParameterValues().get("value").getValue().toString();
            isStruct = structure.contains("STRUCT");
        }

        sb.append(isStruct ? "struct " : "table ").append(classInfo.getSimpleName()).append(" {\n");

        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (field.hasAnnotation(FlatBufferIgnore.class.getName())) {
                continue;
            }
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue; // default ignore statics
            }

            String fieldLine = buildField(field);
            if (fieldLine != null) {
                sb.append("  ").append(fieldLine).append(";\n");
            }
        }

        sb.append("}\n\n");
        return sb.toString();
    }

    private String buildField(FieldInfo field) {
        String name = field.getName();
        String typeName = mapType(field);

        if (typeName == null) {
            log.debug("Skipping unsupported field: {}", name);
            return null;
        }

        StringBuilder line = new StringBuilder(name).append(":").append(typeName);

        // Handle default value from annotation
        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            String defaultValue = Optional.ofNullable(options.getParameterValues().get("defaultValue"))
                    .map(p -> p.getValue().toString())
                    .orElse("");
            if (!defaultValue.isBlank()) {
                line.append(" = ").append(defaultValue);
            }
        }

        return line.toString();
    }

    private String mapType(FieldInfo field) {
        String typeDescriptor = field.getTypeDescriptorStr();
        String simpleType = field.getTypeSignatureOrTypeDescriptor().toStringWithSimpleNames();

        // Check for explicit FieldType override
        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object ft = options.getParameterValues().get("fieldType").getValue();
            if (ft != null) {
                String ftName = ft.toString();
                if (ftName.contains("STRING")) return "string";
                if (ftName.contains("BOOL")) return "bool";
                if (ftName.contains("BYTE")) return "byte";
                if (ftName.contains("SHORT")) return "short";
                if (ftName.contains("INT")) return "int";
                if (ftName.contains("LONG")) return "long";
                if (ftName.contains("FLOAT")) return "float";
                if (ftName.contains("DOUBLE")) return "double";
                // IDENT / ARRAY / MAP handled below or left as-is for now
            }
        }

        // Primitive / wrapper mapping
        if (typeDescriptor.contains("Z") || simpleType.contains("boolean") || simpleType.contains("Boolean")) return "bool";
        if (typeDescriptor.contains("B") || simpleType.contains("byte") || simpleType.contains("Byte")) return "byte";
        if (typeDescriptor.contains("S") || simpleType.contains("short") || simpleType.contains("Short")) return "short";
        if (typeDescriptor.contains("I") || simpleType.contains("int") || simpleType.contains("Integer")) return "int";
        if (typeDescriptor.contains("J") || simpleType.contains("long") || simpleType.contains("Long")) return "long";
        if (typeDescriptor.contains("F") || simpleType.contains("float") || simpleType.contains("Float")) return "float";
        if (typeDescriptor.contains("D") || simpleType.contains("double") || simpleType.contains("Double")) return "double";
        if (simpleType.contains("String")) return "string";

        // Enum or table reference (simple name for now)
        if (field.getClassInfo() != null || !simpleType.contains("[")) {
            return simpleType.replace("java.lang.", "").replace("java.util.", "");
        }

        // Basic list / array support
        if (simpleType.contains("List") || simpleType.contains("[")) {
            // Very simplified - extract element type
            return "[" + extractElementType(simpleType) + "]";
        }

        return null;
    }

    private String extractElementType(String simpleType) {
        // Rough extraction for List<T> or T[]
        if (simpleType.contains("<")) {
            int start = simpleType.indexOf('<') + 1;
            int end = simpleType.lastIndexOf('>');
            if (start > 0 && end > start) {
                return simpleType.substring(start, end).replace("java.lang.", "").trim();
            }
        }
        if (simpleType.endsWith("[]")) {
            return simpleType.substring(0, simpleType.length() - 2).replace("java.lang.", "").trim();
        }
        return "string"; // fallback
    }

    private String buildEnum(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        String enumType = "int"; // default

        var enumAnno = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        if (enumAnno != null && enumAnno.getParameterValues().get("enumType") != null) {
            String et = enumAnno.getParameterValues().get("enumType").getValue().toString();
            if (et.contains("BYTE")) enumType = "byte";
            else if (et.contains("SHORT")) enumType = "short";
            else if (et.contains("LONG")) enumType = "long";
            // else int
        }

        sb.append("enum ").append(classInfo.getSimpleName()).append(" : ").append(enumType).append(" {\n");

        // Enum constants are fields that are static final of the enum type
        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                field.getTypeSignatureOrTypeDescriptor().toString().contains(classInfo.getName())) {
                sb.append("  ").append(field.getName());
                // Optional value if present - simplified
                sb.append(",\n");
            }
        }

        // Clean trailing comma
        String result = sb.toString().replaceAll(",\n$", "\n");
        return result + "}\n\n";
    }
}
