package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.FixedArray;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Unsupported;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import io.github.mhradek.aurkitu.plugin.core.model.StructDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks a schema against FlatBuffers rules. A pure function: it returns a copy with
 * everything that cannot be written removed, plus the issues found, listed in schema order.
 *
 * <p>Corrections: unsupported fields are dropped; ids are dropped from a table where only
 * some fields have one; attributes on fields of the wrong type are dropped; non-scalar
 * struct fields are dropped when {@code dropInvalidStructFields} is set.</p>
 */
final class SchemaValidator {

    record Result(Schema schema, List<String> issues) {}

    private final boolean dropInvalidStructFields;

    SchemaValidator(boolean dropInvalidStructFields) {
        this.dropInvalidStructFields = dropInvalidStructFields;
    }

    Result validate(Schema schema) {
        List<String> issues = new ArrayList<>();
        Map<TypeName, Declaration> declared = declarations(schema, issues);

        String id = schema.fileIdentifier();
        if (id != null && !id.isBlank() && id.length() != 4) {
            issues.add("file_identifier \"" + id + "\" must be exactly 4 characters");
        }

        List<Declaration> valid = new ArrayList<>();
        boolean rootSeen = false;
        for (Declaration declaration : schema.declarations()) {
            if (declaration instanceof CompositeDeclaration composite) {
                valid.add(validate(composite, declared, issues));
                if (composite.root()) {
                    if (rootSeen) {
                        issues.add("Multiple root types; last used: " + composite.name().name());
                    }
                    rootSeen = true;
                }
            } else {
                if (declaration instanceof EnumDeclaration enumDeclaration) {
                    validate(enumDeclaration, issues);
                }
                valid.add(declaration);
            }
        }
        if (!rootSeen) {
            issues.add("No root type defined");
        }
        return new Result(schema.withDeclarations(valid), List.copyOf(issues));
    }

    /** Declarations by name; reports names declared twice. */
    private static Map<TypeName, Declaration> declarations(Schema schema, List<String> issues) {
        Map<TypeName, Declaration> declared = new HashMap<>();
        for (Declaration declaration : schema.declarations()) {
            Declaration previous = declared.putIfAbsent(declaration.name(), declaration);
            if (previous != null) {
                issues.add("Duplicate type name " + declaration.name().qualified()
                        + " (declared by " + previous.source() + " and " + declaration.source() + ")");
            }
        }
        return declared;
    }

    private CompositeDeclaration validate(CompositeDeclaration composite, Map<TypeName, Declaration> declared, List<String> issues) {
        String owner = composite.name().name();
        long withId = composite.fields().stream().filter(f -> f.has(FieldAttribute.Id.class)).count();
        boolean partialIds = withId > 0 && withId < composite.fields().size();
        if (partialIds) {
            issues.add("Partial field ids on " + owner + "; ids omitted (all fields or none must have an id)");
        }

        List<Field> valid = new ArrayList<>();
        for (Field field : composite.fields()) {
            if (field.type() instanceof Unsupported) {
                issues.add("Unsupported field type: " + owner + "." + field.javaName());
                continue;
            }
            Field checked = field.withAttributes(validAttributes(field, owner, partialIds, issues));
            if (composite instanceof StructDeclaration && !isStructCompatible(checked.type(), declared)) {
                issues.add("Struct " + owner + " field " + field.javaName()
                        + " is non-scalar; structs may only contain scalars or other structs");
                if (dropInvalidStructFields) {
                    continue;
                }
            }
            valid.add(checked);
        }
        return composite.withFields(valid);
    }

    private static List<FieldAttribute> validAttributes(Field field, String owner, boolean dropIds, List<String> issues) {
        List<FieldAttribute> valid = new ArrayList<>();
        String where = owner + "." + field.javaName();
        for (FieldAttribute attribute : field.attributes()) {
            if (attribute instanceof FieldAttribute.Id && dropIds) {
                continue;
            }
            if (attribute instanceof FieldAttribute.NestedFlatbuffer && !SchemaType.isByteVector(field.type())) {
                issues.add("nested_flatbuffer on " + where + " skipped: type must be [ubyte]");
                continue;
            }
            if (attribute instanceof FieldAttribute.Flexbuffer && !SchemaType.isByteVector(field.type())) {
                issues.add("flexbuffer on " + where + " skipped: type must be [ubyte]");
                continue;
            }
            if (attribute instanceof FieldAttribute.ForceAlign
                    && !(field.type() instanceof Vector || field.type() instanceof FixedArray)) {
                issues.add("force_align on " + where + " skipped: only valid on vectors");
                continue;
            }
            valid.add(attribute);
        }
        return valid;
    }

    /**
     * Structs may contain scalars, enums, other structs and fixed-length arrays of those.
     * References to types not declared in this schema (e.g. from an include) are given the benefit of the doubt.
     */
    private static boolean isStructCompatible(SchemaType type, Map<TypeName, Declaration> declared) {
        if (type instanceof FixedArray array) {
            return !(array.element() instanceof FixedArray) && isStructCompatible(array.element(), declared);
        }
        if (type instanceof Scalar) {
            return true;
        }
        if (type instanceof Reference reference) {
            Declaration target = declared.get(reference.target());
            return target == null || target instanceof StructDeclaration || target instanceof EnumDeclaration;
        }
        return false; // string, vector
    }

    private static void validate(EnumDeclaration enumDeclaration, List<String> issues) {
        String name = enumDeclaration.name().name();
        if (enumDeclaration.unreadValueField() != null) {
            issues.add("Enum " + name + ": could not read @FlatBufferEnumTypeField "
                    + enumDeclaration.unreadValueField() + "; values are implicit");
        }
        Set<Long> seen = new HashSet<>();
        for (EnumDeclaration.Value value : enumDeclaration.values()) {
            if (value.value() == null) {
                continue;
            }
            if (!seen.add(value.value())) {
                issues.add("Enum " + name + " has duplicate value " + value.value() + " (" + value.name() + ")");
            }
            BigInteger v = new BigInteger(enumDeclaration.format(value.value()));
            if (!enumDeclaration.underlying().contains(v)) {
                issues.add("Enum " + name + " value " + v + " (" + value.name() + ") is out of range for "
                        + enumDeclaration.underlying().keyword());
            }
        }
    }
}
