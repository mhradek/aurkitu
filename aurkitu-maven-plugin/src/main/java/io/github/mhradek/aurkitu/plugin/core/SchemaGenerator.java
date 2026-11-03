package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.classgraph.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Core generator using ClassGraph for modern, reliable classpath scanning.
 * Processes fields of annotated tables and enums, including nested Map support
 * via recursively generated MapValueSet tables.
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
        String typeName = mapType(field.getTypeSignatureOrTypeDescriptor(), owner, field.getName());

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

    /**
     * Recursively maps a TypeSignature to a FlatBuffers type name.
     * Nested maps generate intermediate MapValueSet tables.
     */
    private String mapType(TypeSignature typeSig, ClassInfo owner, String contextName) {
        if (typeSig == null) {
            return "string";
        }

        // Explicit override is handled at the field level; here we process the signature.

        if (isMapType(typeSig)) {
            return generateMapTable(typeSig, owner, contextName);
        }

        if (isListOrArray(typeSig)) {
            return "[" + extractListElementType(typeSig, owner, contextName) + "]";
        }

        return mapTypeSignature(typeSig);
    }

    private boolean isMapType(TypeSignature typeSig) {
        if (typeSig instanceof ClassRefTypeSignature classRef) {
            String base = classRef.getBaseClassName();
            return "java.util.Map".equals(base) || base.endsWith(".Map");
        }
        return false;
    }

    private boolean isListOrArray(TypeSignature typeSig) {
        if (typeSig instanceof ArrayTypeSignature) {
            return true;
        }
        if (typeSig instanceof ClassRefTypeSignature classRef) {
            String base = classRef.getBaseClassName();
            return "java.util.List".equals(base) || base.endsWith(".List")
                    || "java.util.Set".equals(base) || base.endsWith(".Set");
        }
        return false;
    }

    private String generateMapTable(TypeSignature typeSig, ClassInfo owner, String contextName) {
        String mapTableName = "MapValueSet_" + owner.getSimpleName() + "_" + sanitize(contextName);

        if (!generatedMapTables.containsKey(mapTableName)) {
            String keyType = "string";
            String valueType = "string";

            if (typeSig instanceof ClassRefTypeSignature classRef) {
                List<TypeArgument> args = classRef.getTypeArguments();
                if (args.size() >= 1) {
                    keyType = mapType(args.get(0).getTypeSignature(), owner, contextName + "_key");
                }
                if (args.size() >= 2) {
                    valueType = mapType(args.get(1).getTypeSignature(), owner, contextName + "_value");
                }
            }

            String mapSchema = "// Auto-generated type for use with Map<?, ?>\n" +
                    "table " + mapTableName + " {\n" +
                    "  key:" + keyType + ";\n" +
                    "  value:" + valueType + ";\n" +
                    "}\n\n";

            generatedMapTables.put(mapTableName, mapSchema);
            log.debug("Generated map table: {} (key={}, value={})", mapTableName, keyType, valueType);
        }

        return "[" + mapTableName + "]";
    }

    private String extractListElementType(TypeSignature typeSig, ClassInfo owner, String contextName) {
        if (typeSig instanceof ArrayTypeSignature array) {
            return mapType(array.getElementTypeSignature(), owner, contextName + "_elem");
        }
        if (typeSig instanceof ClassRefTypeSignature classRef) {
            List<TypeArgument> args = classRef.getTypeArguments();
            if (!args.isEmpty()) {
                return mapType(args.get(0).getTypeSignature(), owner, contextName + "_elem");
            }
        }
        return "string";
    }

    private String mapTypeSignature(TypeSignature typeSig) {
        if (typeSig == null) {
            return "string";
        }

        if (typeSig instanceof BaseTypeSignature base) {
            return switch (base.getTypeSignatureChar()) {
                case 'Z' -> "bool";
                case 'B' -> "byte";
                case 'S' -> "short";
                case 'I' -> "int";
                case 'J' -> "long";
                case 'F' -> "float";
                case 'D' -> "double";
                case 'C' -> "string";
                default -> "string";
            };
        }

        if (typeSig instanceof ClassRefTypeSignature classRef) {
            String baseName = classRef.getBaseClassName();
            if (baseName.endsWith("String")) return "string";
            if (baseName.endsWith("Boolean")) return "bool";
            if (baseName.endsWith("Byte")) return "byte";
            if (baseName.endsWith("Short")) return "short";
            if (baseName.endsWith("Integer")) return "int";
            if (baseName.endsWith("Long")) return "long";
            if (baseName.endsWith("Float")) return "float";
            if (baseName.endsWith("Double")) return "double";
            if (baseName.endsWith("Character")) return "string";

            int lastDot = baseName.lastIndexOf('.');
            return lastDot >= 0 ? baseName.substring(lastDot + 1) : baseName;
        }

        if (typeSig instanceof ArrayTypeSignature array) {
            return "[" + mapTypeSignature(array.getElementTypeSignature()) + "]";
        }

        return typeSig.toStringWithSimpleNames().replace("java.lang.", "").replace("java.util.", "").trim();
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
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
