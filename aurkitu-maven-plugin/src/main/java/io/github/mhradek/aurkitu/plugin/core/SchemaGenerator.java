package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.annotations.*;
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
 * via recursively generated MapValueSet tables. Supports comments, useFullName,
 * common Java types, and basic validation reporting.
 */
@Slf4j
public class SchemaGenerator {

    private final Config config;
    private final Map<String, String> generatedMapTables = new LinkedHashMap<>();
    private final List<String> validationIssues = new ArrayList<>();

    public static class Config {
        public final MavenProject project;
        public final List<String> specifiedPackages;
        public final boolean validate;
        public final boolean ignoreStaticMembers;
        public final String fileIdentifier;
        public final List<String> includes;
        public final boolean mapCommonTypes;
        public final boolean cacheEnabled;
        public final boolean snakeCaseFields;
        public final boolean generateVersion;
        public final boolean failOnValidationIssues;

        public Config(MavenProject project, List<String> specifiedPackages, boolean validate,
                      boolean ignoreStaticMembers, String fileIdentifier, List<String> includes,
                      boolean mapCommonTypes, boolean cacheEnabled, boolean snakeCaseFields,
                      boolean generateVersion, boolean failOnValidationIssues) {
            this.project = project;
            this.specifiedPackages = specifiedPackages;
            this.validate = validate;
            this.ignoreStaticMembers = ignoreStaticMembers;
            this.fileIdentifier = fileIdentifier;
            this.includes = includes != null ? includes : List.of();
            this.mapCommonTypes = mapCommonTypes;
            this.cacheEnabled = cacheEnabled;
            this.snakeCaseFields = snakeCaseFields;
            this.generateVersion = generateVersion;
            this.failOnValidationIssues = failOnValidationIssues;
        }

        // Convenience constructor for tests
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate) {
            this(project, specifiedPackages, validate, true, null, List.of(), true, false, true, false, false);
        }
    }

    public SchemaGenerator(Config config) {
        this.config = config;
    }

    // Backward-compatible constructor
    public SchemaGenerator(MavenProject project, List<String> specifiedPackages, boolean validate) {
        this(new Config(project, specifiedPackages, validate));
    }

    @SuppressWarnings("PMD")
    public void generate(String schemaName, String namespace, File outputDir) {
        generatedMapTables.clear();
        validationIssues.clear();

        File outFile = new File(outputDir, schemaName + ".fbs");
        File cacheFile = new File(outputDir, schemaName + ".fbs.cache");

        // Simple content-hash cache
        // Hash will be refined after scan; start with config
        String configHash = Integer.toHexString(Objects.hash(
                config.specifiedPackages, config.validate, config.ignoreStaticMembers,
                config.fileIdentifier, config.includes, config.mapCommonTypes, namespace,
                config.snakeCaseFields, config.generateVersion));

        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .enableFieldInfo()
                .enableStaticFinalFieldConstantInitializerValues()
                .ignoreFieldVisibility()
                .acceptPackages(config.specifiedPackages != null && !config.specifiedPackages.isEmpty()
                        ? config.specifiedPackages.toArray(new String[0])
                        : new String[]{"*"})
                .scan()) {

            StringBuilder schema = new StringBuilder();
            schema.append("// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n");
            // Placeholder for version; filled later if generateVersion is true
            schema.append("namespace ").append(namespace).append(";\n\n");

            if (config.fileIdentifier != null && !config.fileIdentifier.isBlank()) {
                schema.append("file_identifier \"").append(config.fileIdentifier).append("\";\n\n");
            }

            for (String include : config.includes) {
                schema.append("include \"").append(include).append("\";\n");
            }
            if (!config.includes.isEmpty()) {
                schema.append("\n");
            }

            // Include scanned type names in the cache hash
            StringBuilder typeNames = new StringBuilder();
            for (ClassInfo ci : scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName())) {
                typeNames.append(ci.getName()).append(';');
            }
            for (ClassInfo ci : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
                typeNames.append(ci.getName()).append(';');
            }
            String inputHash = Integer.toHexString(Objects.hash(configHash, typeNames.toString()));

            if (config.cacheEnabled && outFile.exists() && cacheFile.exists()) {
                try {
                    String cached = Files.readString(cacheFile.toPath()).trim();
                    if (inputHash.equals(cached)) {
                        log.info("Schema cache hit for {}; skipping regeneration", schemaName);
                        return;
                    }
                } catch (IOException ignored) {
                    // fall through
                }
            }

            String rootType = null;

            // Emit enums first so they are defined before tables that reference them
            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
                schema.append(buildEnum(classInfo));
            }

            for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName())) {
                schema.append(buildTable(classInfo));
                if (isRootType(classInfo)) {
                    if (rootType != null) {
                        log.warn("Multiple root types detected; using last one: {}", classInfo.getSimpleName());
                        if (config.validate) {
                            validationIssues.add("Multiple root types; last used: " + classInfo.getSimpleName());
                        }
                    }
                    rootType = classInfo.getSimpleName();
                }
            }

            for (String mapTable : generatedMapTables.values()) {
                schema.append(mapTable);
            }

            if (rootType != null) {
                schema.append("root_type ").append(rootType).append(";\n");
            } else if (config.validate) {
                validationIssues.add("No root type defined");
            }

            if (config.validate && !validationIssues.isEmpty()) {
                schema.append("\n// Validation issues:\n");
                for (String issue : validationIssues) {
                    schema.append("// - ").append(issue).append("\n");
                }
            }

            // Final sanitization for flatc compatibility
            String sanitized = schema.toString().replace('$', '_');

            if (config.generateVersion) {
                // Hash the schema excluding any validation comment block
                String forHash = sanitized;
                int valIdx = forHash.indexOf("\n// Validation issues:");
                if (valIdx > 0) {
                    forHash = forHash.substring(0, valIdx);
                }
                String version = Integer.toHexString(forHash.hashCode());
                sanitized = sanitized.replace(
                        "// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n",
                        "// Aurkitu 1.0.0 automatically generated FlatBuffer schema\n// @version: " + version + "\n");
            }

            Files.writeString(outFile.toPath(), sanitized);
            if (config.cacheEnabled) {
                Files.writeString(cacheFile.toPath(), inputHash);
            }
            log.info("Wrote schema to {}", outFile.getAbsolutePath());

            if (config.validate) {
                if (validationIssues.isEmpty()) {
                    log.info("Basic validation complete. No issues found.");
                } else {
                    log.warn("Validation issues found ({}). See schema comments.", validationIssues.size());
                    if (config.failOnValidationIssues) {
                        throw new SchemaGenerationException("Schema validation failed with " + validationIssues.size() + " issue(s). See schema comments.");
                    }
                }
            }
        } catch (IOException e) {
            throw new SchemaGenerationException("Failed to generate schema", e);
        }
    }

    private boolean isRootType(ClassInfo classInfo) {
        var anno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        return anno != null && Boolean.TRUE.equals(anno.getParameterValues().get("rootType").getValue());
    }

    @SuppressWarnings("PMD")
    private String buildTable(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        boolean isStruct = false;
        var tableAnno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        if (tableAnno != null && tableAnno.getParameterValues().get("value") != null) {
            String structure = String.valueOf(tableAnno.getParameterValues().get("value").getValue());
            isStruct = structure.contains("STRUCT");
        }

        if (classInfo.hasAnnotation(FlatBufferComment.class.getName())) {
            var commentAnno = classInfo.getAnnotationInfo(FlatBufferComment.class.getName());
            String comment = String.valueOf(commentAnno.getParameterValues().get("comment").getValue());
            if (!comment.isBlank()) {
                sb.append("// ").append(comment).append("\n");
            }
        }

        // Per-type namespace override
        if (tableAnno != null && tableAnno.getParameterValues().get("namespace") != null) {
            String ns = String.valueOf(tableAnno.getParameterValues().get("namespace").getValue());
            if (!ns.isBlank()) {
                sb.append("// namespace: ").append(ns).append("\n");
            }
        }

        sb.append(isStruct ? "struct " : "table ").append(classInfo.getSimpleName()).append(" {\n");

        // Collect fields from this class and all superclasses (avoiding duplicates)
        Set<String> seen = new HashSet<>();
        ClassInfo current = classInfo;
        while (current != null) {
            for (FieldInfo field : current.getDeclaredFieldInfo()) {
                String fname = field.getName();
                if (seen.contains(fname)) {
                    continue;
                }
                seen.add(fname);

                if (field.hasAnnotation(FlatBufferIgnore.class.getName())) {
                    continue;
                }
                if (config.ignoreStaticMembers && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                String fieldLine = buildField(field, current);
                if (fieldLine != null) {
                    sb.append("  ").append(fieldLine).append(";\n");
                }
            }
            current = current.getSuperclass();
            // Stop at Object
            if (current != null && "java.lang.Object".equals(current.getName())) {
                break;
            }
        }

        sb.append("}\n\n");
        return sb.toString();
    }

    @SuppressWarnings("PMD")
    private String buildField(FieldInfo field, ClassInfo owner) {
        String name = field.getName();
        String typeName = mapType(field.getTypeSignatureOrTypeDescriptor(), owner, field.getName(), field);

        // Honour explicit fieldType override when present
        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object ft = options.getParameterValues().get("fieldType").getValue();
            if (ft != null) {
                String ftStr = ft.toString();
                // Only override for concrete scalar / string types; leave MAP/ARRAY/IDENT to signature logic
                if (!ftStr.contains("MAP") && !ftStr.contains("ARRAY") && !ftStr.contains("IDENT")) {
                    typeName = ftStr.toLowerCase().replace("fieldtype.", "");
                    if (typeName.contains(".")) {
                        typeName = typeName.substring(typeName.lastIndexOf('.') + 1).toLowerCase();
                    }
                }
                // Unsigned note
                if (ftStr.contains("U") && config.validate) {
                    validationIssues.add("Unsigned type forced on " + owner.getSimpleName() + "." + name +
                            " (" + ft + ") — Java has no unsigned primitives");
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

        StringBuilder line = new StringBuilder();

        if (field.hasAnnotation(FlatBufferComment.class.getName())) {
            var commentAnno = field.getAnnotationInfo(FlatBufferComment.class.getName());
            String comment = String.valueOf(commentAnno.getParameterValues().get("comment").getValue());
            if (!comment.isBlank()) {
                line.append("// ").append(comment).append("\n  ");
            }
        }

        // Sanitize field name
        String safeName = name.replace('$', '_');
        if (config.snakeCaseFields) {
            safeName = toSnakeCase(safeName);
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
            if (Boolean.TRUE.equals(options.getParameterValues().get("required").getValue())) {
                line.append(" (required)");
            }
            if (Boolean.TRUE.equals(options.getParameterValues().get("deprecated").getValue())) {
                line.append(" (deprecated)");
            }
            Object fid = options.getParameterValues().get("fieldId").getValue();
            if (fid instanceof Number n && n.intValue() >= 0) {
                line.append(" (id: ").append(n.intValue()).append(")");
            }
        }

        return line.toString();
    }

    private String mapType(TypeSignature typeSig, ClassInfo owner, String contextName, FieldInfo field) {
        if (typeSig == null) {
            return "string";
        }

        boolean useFullName = false;
        if (field != null && field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            useFullName = Boolean.TRUE.equals(options.getParameterValues().get("useFullName").getValue());
        }

        if (isMapType(typeSig)) {
            return generateMapTable(typeSig, owner, contextName);
        }

        if (isListOrArray(typeSig)) {
            return "[" + extractListElementType(typeSig, owner, contextName) + "]";
        }

        return mapTypeSignature(typeSig, useFullName);
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
        String ctx = contextName;
        if (config.snakeCaseFields) {
            ctx = toSnakeCase(ctx);
        }
        String mapTableName = "MapValueSet_" + owner.getSimpleName() + "_" + sanitize(ctx);

        if (!generatedMapTables.containsKey(mapTableName)) {
            String keyType = "string";
            String valueType = "string";

            if (typeSig instanceof ClassRefTypeSignature classRef) {
                List<TypeArgument> args = classRef.getTypeArguments();
                if (args.size() >= 1) {
                    keyType = mapType(args.get(0).getTypeSignature(), owner, contextName + "_key", null);
                }
                if (args.size() >= 2) {
                    valueType = mapType(args.get(1).getTypeSignature(), owner, contextName + "_value", null);
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
            return mapType(array.getElementTypeSignature(), owner, contextName + "_elem", null);
        }
        if (typeSig instanceof ClassRefTypeSignature classRef) {
            List<TypeArgument> args = classRef.getTypeArguments();
            if (!args.isEmpty()) {
                return mapType(args.get(0).getTypeSignature(), owner, contextName + "_elem", null);
            }
        }
        return "string";
    }

    @SuppressWarnings("PMD")
    private String mapTypeSignature(TypeSignature typeSig, boolean useFullName) {
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

            // Common Java types (optional)
            if (config.mapCommonTypes) {
                if (baseName.endsWith("Date") || baseName.endsWith("LocalDate") ||
                    baseName.endsWith("LocalDateTime") || baseName.endsWith("Instant") ||
                    baseName.endsWith("URL") || baseName.endsWith("URI") ||
                    baseName.endsWith("BigDecimal") || baseName.endsWith("BigInteger") ||
                    baseName.endsWith("UUID")) {
                    return "string";
                }
            }

            if (baseName.endsWith("String")) return "string";
            if (baseName.endsWith("Boolean")) return "bool";
            if (baseName.endsWith("Byte")) return "byte";
            if (baseName.endsWith("Short")) return "short";
            if (baseName.endsWith("Integer")) return "int";
            if (baseName.endsWith("Long")) return "long";
            if (baseName.endsWith("Float")) return "float";
            if (baseName.endsWith("Double")) return "double";
            if (baseName.endsWith("Character")) return "string";

            // Sanitize inner-class $ and package dots
            baseName = baseName.replace('$', '_');
            if (useFullName) {
                return baseName.replace('.', '_');
            }
            int lastDot = baseName.lastIndexOf('.');
            return lastDot >= 0 ? baseName.substring(lastDot + 1) : baseName;
        }

        if (typeSig instanceof ArrayTypeSignature array) {
            String elem = mapTypeSignature(array.getElementTypeSignature(), useFullName);
            // byte[] -> [ubyte] (common binary convention)
            if ("byte".equals(elem)) {
                return "[ubyte]";
            }
            return "[" + elem + "]";
        }

        return typeSig.toStringWithSimpleNames().replace("java.lang.", "").replace("java.util.", "").trim();
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    @SuppressWarnings("PMD")
    private String buildEnum(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        String enumType = "byte";
        boolean isUnion = false;

        var enumAnno = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        if (enumAnno != null) {
            if (enumAnno.getParameterValues().get("enumType") != null) {
                String et = String.valueOf(enumAnno.getParameterValues().get("enumType").getValue());
                if (et.contains("SHORT")) enumType = "short";
                else if (et.contains("INT")) enumType = "int";
                else if (et.contains("LONG")) enumType = "long";
                else if (et.contains("BYTE")) enumType = "byte";
            }
            if (enumAnno.getParameterValues().get("value") != null) {
                String structure = String.valueOf(enumAnno.getParameterValues().get("value").getValue());
                isUnion = structure.contains("UNION");
            }
        }

        if (classInfo.hasAnnotation(FlatBufferComment.class.getName())) {
            var commentAnno = classInfo.getAnnotationInfo(FlatBufferComment.class.getName());
            String comment = String.valueOf(commentAnno.getParameterValues().get("comment").getValue());
            if (!comment.isBlank()) {
                sb.append("// ").append(comment).append("\n");
            }
        }

        if (isUnion) {
            sb.append("union ").append(classInfo.getSimpleName()).append(" {\n");
        } else {
            sb.append("enum ").append(classInfo.getSimpleName()).append(" : ").append(enumType).append(" {\n");
        }

        int ordinal = 0;
        for (FieldInfo field : classInfo.getDeclaredFieldInfo()) {
            String fname = field.getName();
            // Skip synthetic enum $VALUES
            if (fname.startsWith("$") || fname.equals("VALUES")) {
                continue;
            }
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                field.getTypeSignatureOrTypeDescriptor().toString().contains(classInfo.getName())) {
                sb.append("  ").append(fname);
                if (!isUnion) {
                    Object constVal = field.getConstantInitializerValue();
                    if (constVal != null) {
                        sb.append(" = ").append(constVal);
                    } else {
                        // Fallback to declaration order
                        sb.append(" = ").append(ordinal);
                    }
                }
                sb.append(",\n");
                ordinal++;
            }
        }

        String result = sb.toString().replaceAll(",\n$", "\n");
        return result + "}\n\n";
    }

    private String toSnakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
