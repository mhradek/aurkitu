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
        public final String fileExtension;

        @SuppressWarnings("PMD.ExcessiveParameterList")
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate,
                      boolean ignoreStaticMembers, String fileIdentifier, List<String> includes,
                      boolean mapCommonTypes, boolean cacheEnabled, boolean snakeCaseFields,
                      boolean generateVersion, boolean failOnValidationIssues, String fileExtension) {
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
            this.fileExtension = fileExtension;
        }

        // Convenience constructor for tests
        public Config(MavenProject project, List<String> specifiedPackages, boolean validate) {
            this(project, specifiedPackages, validate, true, null, List.of(), true, false, true, false, false, null);
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
            if (config.fileExtension != null && !config.fileExtension.isBlank()) {
                schema.append("file_extension \"").append(config.fileExtension).append("\";\n\n");
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

    private boolean isStruct(ClassInfo classInfo) {
        var tableAnno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        if (tableAnno == null) {
            return false;
        }
        var valueParam = tableAnno.getParameterValues().get("value");
        if (valueParam == null) {
            return false;
        }
        String structure = String.valueOf(valueParam.getValue());
        return structure.toUpperCase().contains("STRUCT");
    }

    @SuppressWarnings("PMD")
    private String buildTable(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        boolean isStruct = isStruct(classInfo);

        var tableAnno = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());

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
                sb.append("namespace ").append(ns).append(";\n");
            }
        }

        sb.append(isStruct ? "struct " : "table ").append(classInfo.getSimpleName()).append(" {\n");

        // Collect fields from this class and all superclasses (avoiding duplicates)
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

                if (field.hasAnnotation(FlatBufferIgnore.class.getName())) {
                    continue;
                }
                if (config.ignoreStaticMembers && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
            current = current.getSuperclass();
            // Stop at Object
            if (current != null && "java.lang.Object".equals(current.getName())) {
                break;
            }
        }

        // FlatBuffers: either all fields or no fields may have an id
        long withId = fields.stream().filter(this::hasFieldId).count();
        boolean emitIds = withId == 0 || withId == fields.size();
        if (withId > 0 && withId < fields.size() && config.validate) {
            validationIssues.add("Partial field ids on " + classInfo.getSimpleName() + "; ids omitted (all fields or none must have an id)");
            log.warn("Partial field ids on {}; omitting id attributes", classInfo.getSimpleName());
        }

        for (FieldInfo field : fields) {
            String fieldLine = buildField(field, classInfo, isStruct, emitIds);
            if (fieldLine != null) {
                if (isStruct && isNonScalarField(fieldLine)) {
                    if (config.validate) {
                        validationIssues.add("Struct " + classInfo.getSimpleName() + " field " + field.getName() + " is non-scalar; structs may only contain scalars or other structs");
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

    private boolean hasFieldId(FieldInfo field) {
        if (!field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            return false;
        }
        var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
        Object fid = options.getParameterValues().get("fieldId").getValue();
        return fid instanceof Number n && n.intValue() >= 0;
    }

    @SuppressWarnings("PMD")
    private String buildField(FieldInfo field, ClassInfo owner, boolean isStruct, boolean emitIds) {
        String name = field.getName();
        String typeName = mapType(field.getTypeSignatureOrTypeDescriptor(), owner, field.getName(), field);
        boolean isArray = typeName != null && typeName.startsWith("[") && typeName.endsWith("]");

        // Honour explicit fieldType override when present
        if (field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object ft = options.getParameterValues().get("fieldType").getValue();
            if (ft != null) {
                String ftStr = ft.toString();
                // Only override for concrete scalar / string types; leave MAP/ARRAY/IDENT to signature logic
                if (!ftStr.contains("MAP") && !ftStr.contains("ARRAY") && !ftStr.contains("IDENT")) {
                    String override = ftStr.toLowerCase().replace("fieldtype.", "");
                    if (override.contains(".")) {
                        override = override.substring(override.lastIndexOf('.') + 1).toLowerCase();
                    }
                    // Preserve array form when the field is an array and the override is a scalar
                    if (isArray && !override.contains("[")) {
                        typeName = "[" + override + "]";
                    } else {
                        typeName = override;
                    }
                }
                // Unsigned types are valid in the schema; note only in debug
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

        // Apply fixedLength rewrite to the type *before* any attributes,
        // and only on structs (fixed arrays are illegal in tables).
        if (isStruct && field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object fl = options.getParameterValues().get("fixedLength").getValue();
            if (fl instanceof Number ln && ln.intValue() > 0) {
                int n = ln.intValue();
                String before = typeName;
                if (typeName.startsWith("[") && typeName.endsWith("]") && !typeName.contains(":")) {
                    // [T] -> [T:N]
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

            // Collect attributes into a single parenthesized group
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
                // nested_flatbuffer is only legal on [ubyte]
                if ("[ubyte]".equals(typeName) || "[byte]".equals(typeName)) {
                    attrs.add("nested_flatbuffer: \"" + nfb + "\"");
                } else if (config.validate) {
                    validationIssues.add("nested_flatbuffer on " + owner.getSimpleName() + "." + name + " skipped: type must be [ubyte]");
                    log.warn("nested_flatbuffer on {}.{} skipped (type is {}, must be [ubyte])", owner.getSimpleName(), name, typeName);
                }
            }
            if (Boolean.TRUE.equals(options.getParameterValues().get("flexbuffer").getValue())) {
                // flexbuffer is only legal on [ubyte]
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

    private String mapType(TypeSignature typeSig, ClassInfo owner, String contextName, FieldInfo field) {
        if (typeSig == null) {
            return "string";
        }

        boolean useFullName = false;
        if (field != null && field.hasAnnotation(FlatBufferFieldOptions.class.getName())) {
            var options = field.getAnnotationInfo(FlatBufferFieldOptions.class.getName());
            Object ufn = options.getParameterValues().get("useFullName").getValue();
            if (Boolean.TRUE.equals(ufn)) {
                useFullName = true;
            }
        }

        // Handle arrays
        if (typeSig instanceof ArrayTypeSignature) {
            ArrayTypeSignature arr = (ArrayTypeSignature) typeSig;
            String element = mapType(arr.getElementTypeSignature(), owner, contextName, field);
            return element == null ? null : "[" + element + "]";
        }

        // Handle ClassRef
        if (typeSig instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature classRef = (ClassRefTypeSignature) typeSig;
            String className = classRef.getFullyQualifiedClassName();

            // Maps
            if ("java.util.Map".equals(className) || className.endsWith(".Map")) {
                List<TypeArgument> typeArgs = classRef.getTypeArguments();
                if (typeArgs.size() == 2) {
                    TypeSignature valueSig = typeArgs.get(1).getTypeSignature();
                    String valueType = mapType(valueSig, owner, contextName, field);
                    if (valueType != null) {
                        String mapTableName = "MapValueSet_" + owner.getSimpleName() + "_" + toSnakeCase(contextName);
                        String mapTable = "table " + mapTableName + " {\n  key:string;\n  value:" + valueType + ";\n}\n\n";
                        generatedMapTables.put(mapTableName, mapTable);
                        return "[" + mapTableName + "]";
                    }
                }
                return null;
            }

            // Lists / Collections
            if (className.endsWith("List") || className.endsWith("Set") || className.endsWith("Collection")) {
                List<TypeArgument> typeArgs = classRef.getTypeArguments();
                if (!typeArgs.isEmpty()) {
                    TypeSignature elementSig = typeArgs.get(0).getTypeSignature();
                    String element = mapType(elementSig, owner, contextName, field);
                    return element == null ? null : "[" + element + "]";
                }
                return null;
            }

            // Primitive wrappers (for map values and list elements)
            String wrapper = mapWrapper(className);
            if (wrapper != null) {
                return wrapper;
            }

            // Common types
            if (config.mapCommonTypes) {
                if ("java.lang.String".equals(className)) return "string";
                if ("java.util.Date".equals(className)) return "string";
                if ("java.net.URL".equals(className)) return "string";
                if ("java.math.BigDecimal".equals(className)) return "string";
            }

            // Enums / tables / structs
            if (className.startsWith("java.")) {
                return null;
            }
            String simple = className.substring(className.lastIndexOf('.') + 1);
            return useFullName ? className.replace('$', '_') : simple;
        }

        // Primitives
        String sig = typeSig.toString();
        return switch (sig) {
            case "boolean" -> "bool";
            case "byte" -> "byte";
            case "short" -> "short";
            case "int" -> "int";
            case "long" -> "long";
            case "float" -> "float";
            case "double" -> "double";
            default -> null;
        };
    }

    private static String mapWrapper(String className) {
        return switch (className) {
            case "java.lang.Boolean" -> "bool";
            case "java.lang.Byte" -> "byte";
            case "java.lang.Short" -> "short";
            case "java.lang.Integer" -> "int";
            case "java.lang.Long" -> "long";
            case "java.lang.Float" -> "float";
            case "java.lang.Double" -> "double";
            default -> null;
        };
    }

    private boolean isNonScalarField(String fieldLine) {
        // Rough check: if the type after the colon contains [ or is not a primitive/scalar
        int colon = fieldLine.indexOf(':');
        if (colon < 0) return false;
        String type = fieldLine.substring(colon + 1);
        int space = type.indexOf(' ');
        if (space > 0) type = type.substring(0, space);
        int paren = type.indexOf('(');
        if (paren > 0) type = type.substring(0, paren);
        return type.contains("[") || type.contains("string") || type.contains("table");
    }

    private String buildEnum(ClassInfo classInfo) {
        StringBuilder sb = new StringBuilder();
        var enumAnno = classInfo.getAnnotationInfo(FlatBufferEnum.class.getName());
        boolean isUnion = false;
        String underlying = "byte";
        if (enumAnno != null) {
            Object val = enumAnno.getParameterValues().get("value").getValue();
            if (val != null && String.valueOf(val).toUpperCase().contains("UNION")) {
                isUnion = true;
            }
            Object et = enumAnno.getParameterValues().get("enumType").getValue();
            if (et != null) {
                String etStr = String.valueOf(et);
                if (etStr.contains("SHORT")) underlying = "short";
                else if (etStr.contains("INT")) underlying = "int";
                else if (etStr.contains("LONG")) underlying = "long";
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
            sb.append("enum ").append(classInfo.getSimpleName()).append(" : ").append(underlying).append(" {\n");
        }

        // Enum constants
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

        // Fallback: if no constants found via ClassGraph, try reflection
        if (sb.toString().endsWith("{\n")) {
            try {
                Class<?> enumClass = Class.forName(classInfo.getName());
                if (enumClass.isEnum()) {
                    Object[] constants = enumClass.getEnumConstants();
                    for (Object c : constants) {
                        sb.append("  ").append(((Enum<?>) c).name()).append(",\n");
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // leave empty
            }
        }

        sb.append("}\n\n");
        return sb.toString();
    }

    private static String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
