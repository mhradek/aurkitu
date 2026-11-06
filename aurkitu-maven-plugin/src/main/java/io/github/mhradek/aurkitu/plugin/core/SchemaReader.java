package io.github.mhradek.aurkitu.plugin.core;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;
import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;
import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.FixedArray;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.StringType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Unsupported;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import io.github.mhradek.aurkitu.plugin.core.model.StructDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TableDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Reads scanned classes into a {@link Schema}. The model is faithful to the annotations;
 * {@link SchemaValidator} then reports and removes anything that cannot be written.
 */
final class SchemaReader {

    private static final String FIELD_OPTIONS = FlatBufferFieldOptions.class.getName();

    private final SchemaGenerator.Config config;
    private final TypeRegistry registry;
    private final Map<String, TableDeclaration> mapTables = new LinkedHashMap<>();
    private final TypeMapper typeMapper;
    private final EnumReader enumReader;

    SchemaReader(SchemaGenerator.Config config, TypeRegistry registry) {
        this.config = config;
        this.registry = registry;
        this.typeMapper = new TypeMapper(config.mapCommonTypes, registry, mapTables);
        this.enumReader = new EnumReader(registry);
    }

    Schema read(ScanResult scanResult) {
        List<Declaration> declarations = new ArrayList<>();
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(FlatBufferEnum.class.getName())) {
            declarations.add(enumReader.read(classInfo));
        }
        List<CompositeDeclaration> composites = new ArrayList<>();
        List<ClassInfo> tables = scanResult.getClassesWithAnnotation(FlatBufferTable.class.getName());
        for (ClassInfo classInfo : tables) {
            composites.add(readComposite(classInfo));
        }
        // Unannotated nested types of annotated tables and structs (see TypeRegistry).
        for (ClassInfo outer : tables) {
            for (ClassInfo nested : TypeRegistry.nestedTypes(outer)) {
                if (nested.isEnum()) {
                    declarations.add(enumReader.read(nested));
                } else {
                    composites.add(readComposite(nested));
                }
            }
        }
        declarations.addAll(declarationOrder(composites));
        declarations.addAll(mapTables.values());
        return new Schema(registry.defaultNamespace(), config.includes, config.fileIdentifier, config.fileExtension, declarations);
    }

    private CompositeDeclaration readComposite(ClassInfo classInfo) {
        AnnotationInfo table = classInfo.getAnnotationInfo(FlatBufferTable.class.getName());
        boolean struct = "STRUCT".equals(Naming.enumConstantName(Naming.param(table, "value")));
        boolean root = Boolean.TRUE.equals(Naming.param(table, "rootType"));
        TypeName name = registry.nameOf(classInfo.getName());
        String comment = comment(classInfo.getAnnotationInfo(FlatBufferComment.class.getName()));

        List<Field> fields = new ArrayList<>();
        for (FieldInfo field : collectFields(classInfo)) {
            fields.add(readField(field, classInfo, struct));
        }
        return struct
                ? new StructDeclaration(name, classInfo.getName(), comment, fields, root)
                : new TableDeclaration(name, classInfo.getName(), comment, fields, root);
    }

    private Field readField(FieldInfo field, ClassInfo owner, boolean inStruct) {
        AnnotationInfo options = field.getAnnotationInfo(FIELD_OPTIONS);
        boolean useFullName = Boolean.TRUE.equals(Naming.param(options, "useFullName"));
        SchemaType type = typeMapper.map(field.getTypeSignatureOrTypeDescriptor(), owner, field.getName(), useFullName);

        type = applyTypeOverride(type, fieldType(options));
        if (type == null) {
            type = new Unsupported(field.getTypeSignatureOrTypeDescriptor().toString());
        }
        if (inStruct && intParam(options, "fixedLength") > 0 && !(type instanceof Unsupported)) {
            int length = intParam(options, "fixedLength");
            type = type instanceof Vector v ? new FixedArray(v.element(), length)
                    : type instanceof FixedArray ? type
                    : new FixedArray(type, length);
        }

        return new Field(field.getName(), Naming.fieldName(field.getName(), config.snakeCaseFields), type,
                Naming.stringParam(options, "defaultValue"),
                comment(field.getAnnotationInfo(FlatBufferComment.class.getName())),
                attributes(options));
    }

    /**
     * {@code fieldType} replaces the mapped type with a scalar or string (keeping a vector a vector),
     * which is how unsigned types are expressed. MAP, ARRAY and IDENT keep the mapped type.
     */
    private static SchemaType applyTypeOverride(SchemaType mapped, FieldType fieldType) {
        if (fieldType == null || fieldType == FieldType.MAP || fieldType == FieldType.ARRAY || fieldType == FieldType.IDENT) {
            return mapped;
        }
        SchemaType override = fieldType == FieldType.STRING ? StringType.INSTANCE : new Scalar(ScalarKind.valueOf(fieldType.name()));
        return mapped instanceof Vector ? new Vector(override) : override;
    }

    /** Attributes in the order they are written. Applicability is checked by the validator. */
    private static List<FieldAttribute> attributes(AnnotationInfo options) {
        if (options == null) {
            return List.of();
        }
        Object fieldId = Naming.param(options, "fieldId");
        String hash = Naming.stringParam(options, "hash");
        String nested = Naming.stringParam(options, "nestedFlatbuffer");
        int forceAlign = intParam(options, "forceAlign");

        List<FieldAttribute> attributes = new ArrayList<>();
        addIf(attributes, flag(options, "required"), FieldAttribute.Required::new);
        addIf(attributes, flag(options, "deprecated"), FieldAttribute.Deprecated::new);
        addIf(attributes, fieldId instanceof Number n && n.intValue() >= 0, () -> new FieldAttribute.Id(((Number) fieldId).intValue()));
        addIf(attributes, flag(options, "key"), FieldAttribute.Key::new);
        addIf(attributes, !hash.isBlank(), () -> new FieldAttribute.Hash(hash));
        addIf(attributes, !nested.isBlank(), () -> new FieldAttribute.NestedFlatbuffer(nested));
        addIf(attributes, flag(options, "flexbuffer"), FieldAttribute.Flexbuffer::new);
        addIf(attributes, forceAlign > 0, () -> new FieldAttribute.ForceAlign(forceAlign));
        return attributes;
    }

    private static void addIf(List<FieldAttribute> attributes, boolean condition, Supplier<FieldAttribute> attribute) {
        if (condition) {
            attributes.add(attribute.get());
        }
    }

    private static boolean flag(AnnotationInfo options, String name) {
        return Boolean.TRUE.equals(Naming.param(options, name));
    }

    /** Declared fields, then inherited ones; skips ignored, synthetic and (optionally) static fields. */
    private List<FieldInfo> collectFields(ClassInfo classInfo) {
        List<FieldInfo> fields = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ClassInfo current = classInfo;
             current != null && !"java.lang.Object".equals(current.getName());
             current = current.getSuperclass()) {
            for (FieldInfo field : current.getDeclaredFieldInfo()) {
                if (!seen.add(field.getName())) {
                    continue;
                }
                // Synthetic: compiler-generated fields, e.g. this$0 on non-static inner classes.
                if (field.isSynthetic() || field.hasAnnotation(FlatBufferIgnore.class.getName())) {
                    continue;
                }
                if (config.ignoreStaticMembers && Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                fields.add(field);
            }
        }
        return fields;
    }

    /**
     * Structs first, each after the structs it contains (flatc needs a struct's size when
     * it parses it), then tables, which may reference anything. Otherwise scan order is kept.
     */
    static List<CompositeDeclaration> declarationOrder(List<CompositeDeclaration> composites) {
        Map<TypeName, StructDeclaration> structs = new LinkedHashMap<>();
        List<CompositeDeclaration> tables = new ArrayList<>();
        for (CompositeDeclaration composite : composites) {
            if (composite instanceof StructDeclaration struct) {
                structs.put(struct.name(), struct);
            } else {
                tables.add(composite);
            }
        }
        List<CompositeDeclaration> ordered = new ArrayList<>();
        Set<TypeName> visited = new HashSet<>();
        for (StructDeclaration struct : structs.values()) {
            visitStruct(struct, structs, visited, ordered);
        }
        ordered.addAll(tables);
        return ordered;
    }

    private static void visitStruct(StructDeclaration struct, Map<TypeName, StructDeclaration> structs,
                                    Set<TypeName> visited, List<CompositeDeclaration> ordered) {
        if (!visited.add(struct.name())) {
            return; // done, or a cycle (which flatc will report)
        }
        for (Field field : struct.fields()) {
            SchemaType type = field.type() instanceof FixedArray array ? array.element() : field.type();
            if (type instanceof Reference ref && structs.containsKey(ref.target())) {
                visitStruct(structs.get(ref.target()), structs, visited, ordered);
            }
        }
        ordered.add(struct);
    }

    private static FieldType fieldType(AnnotationInfo options) {
        String constant = Naming.enumConstantName(Naming.param(options, "fieldType"));
        try {
            return constant.isEmpty() ? null : FieldType.valueOf(constant);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int intParam(AnnotationInfo options, String name) {
        return Naming.param(options, name) instanceof Number n ? n.intValue() : 0;
    }

    static String comment(AnnotationInfo commentAnnotation) {
        return Naming.stringParam(commentAnnotation, "comment");
    }
}
