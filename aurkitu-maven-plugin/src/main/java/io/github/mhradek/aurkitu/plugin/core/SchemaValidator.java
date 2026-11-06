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
import io.github.mhradek.aurkitu.plugin.core.model.TableDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;
import io.github.mhradek.aurkitu.plugin.core.model.UnionDeclaration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Checks a schema against FlatBuffers rules. A pure function: it returns a copy with
 * everything that cannot be written removed, plus the issues found, listed in schema order.
 *
 * <p>Corrections: unsupported fields are dropped; ids are dropped from a table where only
 * some fields have one; attributes on fields of the wrong type are dropped; non-scalar
 * struct fields are dropped when {@code dropInvalidStructFields} is set; a struct marked as
 * root loses the mark (flatc only accepts a table as root).</p>
 */
final class SchemaValidator {

    record Result(Schema schema, List<String> issues) {}

    private static final Pattern NAMESPACE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

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

        checkNamespaces(schema, issues);
        boolean checkReferences = schema.includes().isEmpty(); // included types cannot be resolved here

        List<Declaration> valid = new ArrayList<>();
        boolean rootSeen = false;
        for (Declaration declaration : schema.declarations()) {
            if (declaration instanceof StructDeclaration struct && struct.root()) {
                issues.add("Root type " + struct.name().name() + " is a struct; root_type must be a table");
                declaration = new StructDeclaration(struct.name(), struct.source(), struct.comment(), struct.fields(), false);
                rootSeen = true; // reported above; not also "No root type defined"
            }
            if (declaration instanceof CompositeDeclaration composite) {
                valid.add(validate(composite, declared, checkReferences, issues));
                if (composite.root()) {
                    if (rootSeen) {
                        issues.add("Multiple root types; last used: " + composite.name().name());
                    }
                    rootSeen = true;
                }
            } else {
                if (declaration instanceof EnumDeclaration enumDeclaration) {
                    validate(enumDeclaration, issues);
                } else if (declaration instanceof UnionDeclaration union) {
                    validate(union, declared, checkReferences, issues);
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

    private CompositeDeclaration validate(CompositeDeclaration composite, Map<TypeName, Declaration> declared,
                                          boolean checkReferences, List<String> issues) {
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
            if (composite instanceof TableDeclaration) {
                checkEnumDefault(checked, owner, declared, issues);
            }
            if (checkReferences) {
                checkReferenceDeclared(checked, owner, declared, issues);
            }
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

    /**
     * A table field of enum type defaults to 0 unless a default is given, and flatc rejects a
     * default that is not one of the enum's values. Reported rather than guessed, since picking
     * a default would change what an absent field means.
     */
    private static void checkEnumDefault(Field field, String owner, Map<TypeName, Declaration> declared, List<String> issues) {
        if (!(field.type() instanceof Reference reference)
                || !(declared.get(reference.target()) instanceof EnumDeclaration enumDeclaration)
                || enumDeclaration.values().isEmpty()) {
            return;
        }
        Set<String> names = new HashSet<>();
        Set<Long> values = new HashSet<>();
        List<EnumDeclaration.Value> constants = enumDeclaration.values();
        for (int i = 0; i < constants.size(); i++) {
            names.add(constants.get(i).name());
            values.add(constants.get(i).value() != null ? constants.get(i).value() : i); // implicit: 0, 1, 2...
        }
        String enumName = enumDeclaration.name().name();
        String where = owner + "." + field.javaName();
        String defaultValue = field.defaultValue().trim();
        if (defaultValue.isEmpty()) {
            if (!values.contains(0L)) {
                issues.add("Field " + where + " needs a defaultValue: enum " + enumName
                        + " has no value 0, which is the default otherwise");
            }
        } else if (!names.contains(defaultValue) && !values.contains(parseLong(defaultValue))) {
            issues.add("Field " + where + " default " + defaultValue + " is not a value of enum " + enumName);
        }
    }

    /** A referenced type must be declared in this schema (unless it may come from an include). */
    private static void checkReferenceDeclared(Field field, String owner, Map<TypeName, Declaration> declared, List<String> issues) {
        SchemaType type = field.type();
        if (type instanceof Vector vector) {
            type = vector.element();
        } else if (type instanceof FixedArray array) {
            type = array.element();
        }
        if (type instanceof Reference reference && !declared.containsKey(reference.target())) {
            issues.add("Field " + owner + "." + field.javaName() + " references " + reference.target().qualified()
                    + ", which is not declared in this schema; annotate it, or include the schema that declares it");
        }
    }

    /**
     * Union members name tables (flatc also accepts structs), resolved in the union's namespace.
     * An enum or union can never be a member.
     */
    private static void validate(UnionDeclaration union, Map<TypeName, Declaration> declared, boolean checkReferences,
                                 List<String> issues) {
        String name = union.name().name();
        if (union.members().isEmpty()) {
            issues.add("Union " + name + " has no members");
        }
        for (String member : union.members()) {
            Declaration target = declared.get(new TypeName(union.name().namespace(), member));
            if (target == null && checkReferences) {
                issues.add("Union " + name + " member " + member + " is not declared in this schema");
            } else if (target instanceof EnumDeclaration || target instanceof UnionDeclaration) {
                issues.add("Union " + name + " member " + member + " is not a table");
            }
        }
    }

    /** Namespaces are dot-separated identifiers. */
    private static void checkNamespaces(Schema schema, List<String> issues) {
        Set<String> namespaces = new LinkedHashSet<>();
        namespaces.add(schema.namespace());
        schema.declarations().forEach(d -> namespaces.add(d.name().namespace()));
        for (String namespace : namespaces) {
            if (!NAMESPACE.matcher(namespace).matches()) {
                issues.add("Namespace \"" + namespace + "\" is not valid: use dot-separated identifiers (letters, digits, _)");
            }
        }
    }

    private static Long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return null;
        }
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
        if (enumDeclaration.values().isEmpty()) {
            issues.add("Enum " + name + " has no values");
        }
        if (enumDeclaration.valuesProblem() != null) {
            issues.add("Enum " + name + ": " + enumDeclaration.valuesProblem() + "; values are implicit");
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
