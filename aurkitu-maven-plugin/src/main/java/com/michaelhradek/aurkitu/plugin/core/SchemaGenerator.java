package com.michaelhradek.aurkitu.plugin.core;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Core generator using ClassGraph for modern, reliable classpath scanning.
 * Processes fields of annotated tables and enums, including Map support via generated MapValueSet tables.
 */
@Slf4j
public class SchemaGenerator {

    private final MavenProject project;
    private final List<String> specifiedPackages;
    private final boolean validate;

    // Collected auto-generated map tables: name -> schema fragment
    private final Map<String, String> generatedMapTables = new LinkedHashMap<>();

    public SchemaGenerator(MavenProject project, List<String> specifiedPackages, boolean validate) {
        this.project = project;
        this.specifiedPackages = specifiedPackages;
        this.validate = validate;
    }

    public void generate(String schemaName, String namespace, File outputDir) {
        generatedMapTables.clear();

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

            // Emit auto-generated MapValueSet tables
            for (String mapTable : generatedMapTables.values()) {
                schema.append(mapTable);
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
        var anno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        return anno != null && Boolean.TRUE.equals(anno.getParameterValues().get("rootType").getValue());
    }

    private String buildTable(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        boolean isStruct = false;
        var tableAnno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        if (tableAnno != null && tableAnno.getParameterValues().get("value") != null) {
            String structure = String.valueOf(tableAnno.getParameterValues().get("value").getValue());
            isStruct = structure.contains("STRUCT");
        }

        sb.append(isStruct ? "struct " : "table ").append(classInfo.getSimpleName()).append(" {\n");

        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (field.hasAnnotation(FlatBufferIgnore.class.getName())) {
                continue;
            }
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String fieldLine = buildField(field, classInfo);
            if (fieldLine != null) {
                sb.append("  ").append(fieldLine).append(";\n");
            }
        }

        sb.append("}\n\n");
        return sb.toString();
    }

    private String buildField(FieldInfo field, ClassInfo owner) {
        String name = field.getName();
        String typeName = mapType(field, owner);

        if (typeName == null) {
            log.debug("Skipping unsupported field: {}", name);
            return null;
        }

        StringBuilder line = new StringBuilder(name).append(":").append(typeName);

        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            String defaultValue = Optional.ofNullable(options.getParameterValues().get("defaultValue"))
                    .map(p -> String.valueOf(p.getValue()))
                    .orElse("");
            if (!defaultValue.isBlank()) {
                line.append(" = ").append(defaultValue);
            }
        }

        return line.toString();
    }

    private String mapType(FieldInfo field, ClassInfo owner) {
        String simpleType = field.getTypeSignatureOrTypeDescriptor().toStringWithSimpleNames();

        // Explicit FieldType override (simplified)
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
            }
        }

        // Map support — generate MapValueSet table and return list reference
        if (simpleType.contains("Map<") || simpleType.contains("java.util.Map")) {
            return generateMapTable(field, owner, simpleType);
        }

        // Primitive / wrapper mapping
        if (simpleType.contains("boolean") || simpleType.contains("Boolean")) return "bool";
        if (simpleType.contains("byte") || simpleType.contains("Byte")) return "byte";
        if (simpleType.contains("short") || simpleType.contains("Short")) return "short";
        if (simpleType.contains("int") || simpleType.contains("Integer")) return "int";
        if (simpleType.contains("long") || simpleType.contains("Long")) return "long";
        if (simpleType.contains("float") || simpleType.contains("Float")) return "float";
        if (simpleType.contains("double") || simpleType.contains("Double")) return "double";
        if (simpleType.contains("String")) return "string";

        // Basic list / array
        if (simpleType.contains("List<") || simpleType.contains("[]")) {
            return "[" + extractElementType(simpleType) + "]";
        }

        // Table / enum reference
        return simpleType.replace("java.lang.", "").replace("java.util.", "").trim();
    }

    private String generateMapTable(FieldInfo field, ClassInfo owner, String simpleType) {
        String mapTableName = "MapValueSet_" + owner.getSimpleName() + "_" + field.getName();

        if (!generatedMapTables.containsKey(mapTableName)) {
            String keyType = extractMapType(simpleType, true);
            String valueType = extractMapType(simpleType, false);

            String mapSchema = "// Auto-generated type for use with Map<?, ?>\n" +
                    "table " + mapTableName + " {\n" +
                    "  key:" + keyType + ";\n" +
                    "  value:" + valueType + ";\n" +
                    "}\n\n";

            generatedMapTables.put(mapTableName, mapSchema);
            log.debug("Generated map table: {}", mapTableName);
        }

        return "[" + mapTableName + "]";
    }

    private String extractMapType(String simpleType, boolean key) {
        // Rough parse of Map<K, V>
        int start = simpleType.indexOf('<');
        int end = simpleType.lastIndexOf('>');
        if (start < 0 || end < 0 || end <= start) {
            return "string";
        }
        String params = simpleType.substring(start + 1, end);
        String[] parts = params.split(",");
        if (parts.length < 2) {
            return "string";
        }
        String raw = (key ? parts[0] : parts[1]).trim();
        return mapSimpleType(raw);
    }

    private String mapSimpleType(String raw) {
        String t = raw.replace("java.lang.", "").replace("java.util.", "").trim();
        if (t.contains("String")) return "string";
        if (t.contains("Boolean") || t.contains("boolean")) return "bool";
        if (t.contains("Byte") || t.contains("byte")) return "byte";
        if (t.contains("Short") || t.contains("short")) return "short";
        if (t.contains("Integer") || t.contains("int")) return "int";
        if (t.contains("Long") || t.contains("long")) return "long";
        if (t.contains("Float") || t.contains("float")) return "float";
        if (t.contains("Double") || t.contains("double")) return "double";
        return t; // enum or table name
    }

    private String extractElementType(String simpleType) {
        if (simpleType.contains("<")) {
            int start = simpleType.indexOf('<') + 1;
            int end = simpleType.lastIndexOf('>');
            if (start > 0 && end > start) {
                return mapSimpleType(simpleType.substring(start, end));
            }
        }
        if (simpleType.endsWith("[]")) {
            return mapSimpleType(simpleType.substring(0, simpleType.length() - 2));
        }
        return "string";
    }

    private String buildEnum(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        String enumType = "int";

        var enumAnno = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        if (enumAnno != null && enumAnno.getParameterValues().get("enumType") != null) {
            String et = String.valueOf(enumAnno.getParameterValues().get("enumType").getValue());
            if (et.contains("BYTE")) enumType = "byte";
            else if (et.contains("SHORT")) enumType = "short";
            else if (et.contains("LONG")) enumType = "long";
        }

        sb.append("enum ").append(classInfo.getSimpleName()).append(" : ").append(enumType).append(" {\n");

        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                field.getTypeSignatureOrTypeDescriptor().toString().contains(classInfo.getName())) {
                sb.append("  ").append(field.getName()).append(",\n");
            }
        }

        String result = sb.toString().replaceAll(",\n$", "\n");
        return result + "}\n\n";
    }
}
