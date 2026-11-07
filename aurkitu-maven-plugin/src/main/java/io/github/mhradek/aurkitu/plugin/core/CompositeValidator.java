package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
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
import io.github.mhradek.aurkitu.plugin.core.model.UnionDeclaration;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Checks the fields of one table or struct against the FlatBuffers schema rules
 * (https://flatbuffers.dev/schema/), returning a corrected copy. Problems flatc would reject
 * are issues; risks to compatibility with existing data are warnings.
 */
final class CompositeValidator {

    /** Hash algorithms flatc accepts, by the integer width they produce. */
    private static final Map<String, Integer> HASHES = Map.of(
            "fnv1_32", 32, "fnv1a_32", 32, "fnv1_64", 64, "fnv1a_64", 64);

    private static final Pattern INTEGER = Pattern.compile("[-+]?(0[xX][0-9a-fA-F]+|[0-9]+)");
    private static final Pattern FLOAT = Pattern.compile(
            "[-+]?((\\.[0-9]+|[0-9]+\\.[0-9]*|[0-9]+)([eE][-+]?[0-9]+)?"
                    + "|0[xX](\\.[0-9a-fA-F]+|[0-9a-fA-F]+\\.[0-9a-fA-F]*|[0-9a-fA-F]+)[pP][-+]?[0-9]+"
                    + "|nan|inf|infinity)");

    private final Map<TypeName, Declaration> declared;
    private final boolean checkReferences;
    private final boolean dropInvalidStructFields;
    private final List<String> issues;
    private final List<String> warnings;

    CompositeValidator(Map<TypeName, Declaration> declared, boolean checkReferences, boolean dropInvalidStructFields,
                       List<String> issues, List<String> warnings) {
        this.declared = declared;
        this.checkReferences = checkReferences;
        this.dropInvalidStructFields = dropInvalidStructFields;
        this.issues = issues;
        this.warnings = warnings;
    }

    CompositeDeclaration validate(CompositeDeclaration composite) {
        String owner = composite.name().name();
        boolean struct = composite instanceof StructDeclaration;
        boolean dropIds = !idsValid(composite, owner);

        List<Field> valid = new ArrayList<>();
        String key = null;
        for (Field field : composite.fields()) {
            if (field.type() instanceof Unsupported) {
                issues.add("Unsupported field type: " + owner + "." + field.javaName());
                continue;
            }
            String where = owner + "." + field.javaName();
            Field checked = field.withAttributes(validAttributes(field, where, struct, dropIds, key != null ? key : ""));
            if (checked.has(FieldAttribute.Key.class)) {
                key = field.javaName();
            }
            checked = checkDefault(checked, where, struct);
            if (checkReferences) {
                checkReferenceDeclared(checked, where);
            }
            if (!struct && checked.type() instanceof FixedArray array) {
                issues.add("fixedLength on " + where + " ignored: fixed-length arrays are only allowed in structs;"
                        + " written as a vector");
                checked = checked.withType(new Vector(array.element()));
            }
            if (isUnion(elementOf(checked.type())) && checked.type() instanceof Vector) {
                issues.add("Field " + where + " is a vector of unions, which flatc supports only for C++");
            }
            if (struct && !isStructCompatible(checked.type())) {
                issues.add("Struct " + owner + " field " + field.javaName()
                        + " is non-scalar; structs may only contain scalars or other structs");
                if (dropInvalidStructFields) {
                    continue;
                }
            }
            valid.add(checked);
        }
        checkNames(owner, valid);
        if (!struct) {
            warnAboutInheritedFields((TableDeclaration) composite, valid);
        }
        return composite.withFields(valid);
    }

    /**
     * Ids must be on all fields or none, unique and contiguous from 0; a union field also takes
     * the id before its own (for its hidden type field). Otherwise all ids are dropped.
     */
    private boolean idsValid(CompositeDeclaration composite, String owner) {
        List<Field> fields = composite.fields();
        List<Field> withId = fields.stream().filter(f -> f.has(FieldAttribute.Id.class)).toList();
        if (withId.isEmpty()) {
            return true;
        }
        if (withId.size() < fields.size()) {
            issues.add("Partial field ids on " + owner + "; ids omitted (all fields or none must have an id)");
            return false;
        }
        TreeSet<Integer> used = new TreeSet<>();
        boolean unique = true;
        for (Field field : fields) {
            int id = idOf(field);
            unique &= used.add(id);
            if (isUnion(field.type())) {
                unique &= id > 0 && used.add(id - 1);
            }
        }
        boolean contiguous = used.first() == 0 && used.last() == used.size() - 1;
        if (!unique || !contiguous) {
            issues.add("Field ids on " + owner + " must be unique and contiguous from 0, and a union field also"
                    + " takes the id before its own; ids omitted (used: " + used + ")");
            return false;
        }
        return true;
    }

    private static int idOf(Field field) {
        return field.attributes().stream().filter(FieldAttribute.Id.class::isInstance)
                .map(a -> ((FieldAttribute.Id) a).value()).findFirst().orElse(-1);
    }

    private List<FieldAttribute> validAttributes(Field field, String where, boolean struct, boolean dropIds, String key) {
        List<FieldAttribute> valid = new ArrayList<>();
        SchemaType type = field.type();
        for (FieldAttribute attribute : field.attributes()) {
            String problem = problem(attribute, type, struct, dropIds, key);
            if (problem == null) {
                valid.add(attribute);
            } else if (!problem.isEmpty()) {
                issues.add(problem.replace("{}", where));
            }
        }
        return valid;
    }

    /** Why an attribute cannot stay (with {} for the field), "" to drop it silently, or null to keep it. */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    private String problem(FieldAttribute attribute, SchemaType type, boolean struct, boolean dropIds, String key) {
        if (attribute instanceof FieldAttribute.Id) {
            return dropIds ? "" : null;
        }
        if (attribute instanceof FieldAttribute.NestedFlatbuffer nested) {
            if (!SchemaType.isByteVector(type)) {
                return "nested_flatbuffer on {} skipped: type must be [ubyte]";
            }
            return checkReferences && findTable(nested.rootType()) == null
                    ? "nested_flatbuffer on {} skipped: " + nested.rootType() + " is not a table declared in this schema"
                    : null;
        }
        if (attribute instanceof FieldAttribute.Flexbuffer) {
            return SchemaType.isByteVector(type) ? null : "flexbuffer on {} skipped: type must be [ubyte]";
        }
        if (attribute instanceof FieldAttribute.ForceAlign) {
            return type instanceof Vector || type instanceof FixedArray ? null : "force_align on {} skipped: only valid on vectors";
        }
        if (attribute instanceof FieldAttribute.Required) {
            if (struct) {
                return "required on {} skipped: struct fields are always present";
            }
            return isScalarLike(type) ? "required on {} skipped: only strings, vectors, tables and structs can be required" : null;
        }
        if (attribute instanceof FieldAttribute.Deprecated) {
            return struct ? "deprecated on {} skipped: struct fields cannot be deprecated" : null;
        }
        if (attribute instanceof FieldAttribute.Key) {
            if (!(type instanceof StringType) && !isScalarLike(type)) {
                return "key on {} skipped: a key must be a string, scalar or enum";
            }
            return key.isEmpty() ? null : "key on {} skipped: only one key is allowed (already on " + key + ")";
        }
        if (attribute instanceof FieldAttribute.Hash hash) {
            return hashProblem(hash, type);
        }
        return null;
    }

    private static String hashProblem(FieldAttribute.Hash hash, SchemaType type) {
        Integer width = HASHES.get(hash.algorithm());
        if (width == null) {
            return "hash on {} skipped: " + hash.algorithm() + " is not one of " + new TreeSet<>(HASHES.keySet());
        }
        boolean matches = type instanceof Scalar s && switch (s.kind()) {
            case INT, UINT -> width == 32;
            case LONG, ULONG -> width == 64;
            default -> false;
        };
        return matches ? null : "hash on {} skipped: " + hash.algorithm() + " needs a " + width + "-bit integer field";
    }

    /** Defaults: only on scalars and enums in tables, never with required, and the value must fit the type. */
    private Field checkDefault(Field field, String where, boolean struct) {
        String value = field.defaultValue().trim();
        EnumDeclaration enumType = enumOf(field.type());
        if (value.isEmpty()) {
            if (!struct && enumType != null) {
                checkEnumZero(enumType, where);
            }
            return field;
        }
        String problem = struct ? "struct fields cannot have defaults"
                : field.has(FieldAttribute.Required.class) ? "a required field cannot have a default"
                : !isScalarLike(field.type()) ? "only scalar and enum fields can have defaults"
                : "null".equals(value) ? null // optional scalar or enum
                : enumType != null ? enumDefaultProblem(enumType, value)
                : scalarDefaultProblem(((Scalar) field.type()).kind(), value);
        if (problem == null) {
            return field;
        }
        if (enumType != null && !struct && !field.has(FieldAttribute.Required.class)) {
            issues.add("Field " + where + " default " + value + " is not a value of enum " + enumType.name().name());
            return field;
        }
        issues.add("default " + value + " on " + where + " skipped: " + problem);
        return field.withDefaultValue("");
    }

    /** A table field of enum type defaults to 0, which flatc rejects if 0 is not a value of the enum. */
    private void checkEnumZero(EnumDeclaration enumType, String where) {
        if (!enumType.values().isEmpty() && !enumValues(enumType).contains(0L)) {
            issues.add("Field " + where + " needs a defaultValue: enum " + enumType.name().name()
                    + " has no value 0, which is the default otherwise");
        }
    }

    private static String enumDefaultProblem(EnumDeclaration enumType, String value) {
        boolean name = enumType.values().stream().anyMatch(v -> v.name().equals(value));
        Long number = INTEGER.matcher(value).matches() ? parseInteger(value).longValue() : null;
        return name || enumValues(enumType).contains(number) ? null : "not a value of the enum";
    }

    private static Set<Long> enumValues(EnumDeclaration enumType) {
        Set<Long> values = new HashSet<>();
        List<EnumDeclaration.Value> constants = enumType.values();
        for (int i = 0; i < constants.size(); i++) {
            values.add(constants.get(i).value() != null ? constants.get(i).value() : i); // implicit: 0, 1, 2...
        }
        return values;
    }

    static String scalarDefaultProblem(ScalarKind kind, String value) {
        if (kind == ScalarKind.BOOL) {
            return value.equals("true") || value.equals("false") || value.equals("0") || value.equals("1")
                    ? null : "not a bool (true or false)";
        }
        if (kind.isIntegral()) {
            if (!INTEGER.matcher(value).matches()) {
                return "not an integer";
            }
            return kind.contains(parseInteger(value)) ? null : "out of range for " + kind.keyword();
        }
        return FLOAT.matcher(value.toLowerCase()).matches() ? null : "not a number";
    }

    private static BigInteger parseInteger(String value) {
        String digits = value.replaceFirst("^[+]", "");
        boolean negative = digits.startsWith("-");
        String unsigned = negative ? digits.substring(1) : digits;
        BigInteger magnitude = unsigned.toLowerCase().startsWith("0x")
                ? new BigInteger(unsigned.substring(2), 16) : new BigInteger(unsigned);
        return negative ? magnitude.negate() : magnitude;
    }

    /** A referenced type must be declared in this schema (unless it may come from an include). */
    private void checkReferenceDeclared(Field field, String where) {
        if (elementOf(field.type()) instanceof Reference reference && !declared.containsKey(reference.target())) {
            issues.add("Field " + where + " references " + reference.target().qualified()
                    + ", which is not declared in this schema; annotate it, or include the schema that declares it");
        }
    }

    /** Field names must be unique, including the hidden {@code <name>_type} field of each union field. */
    private void checkNames(String owner, List<Field> fields) {
        Map<String, String> byName = new HashMap<>();
        for (Field field : fields) {
            String previous = byName.putIfAbsent(field.name(), field.javaName());
            if (previous != null) {
                issues.add("Fields " + owner + "." + previous + " and " + owner + "." + field.javaName()
                        + " are both written as " + field.name());
            }
        }
        for (Field field : fields) {
            if (isUnion(field.type()) && byName.containsKey(field.name() + "_type")) {
                issues.add("Field " + owner + "." + byName.get(field.name() + "_type") + " clashes with the hidden "
                        + field.name() + "_type field flatc adds for union field " + field.javaName());
            }
        }
    }

    /**
     * Inherited fields are written after the class's own fields, so adding a field to the class moves
     * them and existing data no longer reads correctly. Field ids fix each field's position.
     */
    private void warnAboutInheritedFields(TableDeclaration table, List<Field> fields) {
        boolean ids = !fields.isEmpty() && fields.stream().allMatch(f -> f.has(FieldAttribute.Id.class));
        Set<String> parents = new LinkedHashSet<>();
        for (Field field : fields) {
            if (!field.declaredIn().isEmpty() && !field.declaredIn().equals(table.source())) {
                parents.add(Naming.typeName(field.declaredIn()));
            }
        }
        if (!ids && !parents.isEmpty()) {
            warnings.add("Table " + table.name().name() + " has inherited fields (from " + String.join(", ", parents)
                    + ") after its own; adding a field to " + Naming.typeName(table.source())
                    + " moves them and breaks existing data. Give every field a fieldId to fix their positions.");
        }
    }

    /**
     * Structs may contain scalars, enums, other structs and fixed-length arrays of those.
     * References to types not declared in this schema (e.g. from an include) are given the benefit of the doubt.
     */
    private boolean isStructCompatible(SchemaType type) {
        if (type instanceof FixedArray array) {
            return !(array.element() instanceof FixedArray) && isStructCompatible(array.element());
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

    /** Scalars and enums: the types that take defaults and cannot be required. */
    private boolean isScalarLike(SchemaType type) {
        return type instanceof Scalar || enumOf(type) != null;
    }

    private EnumDeclaration enumOf(SchemaType type) {
        return type instanceof Reference reference && declared.get(reference.target()) instanceof EnumDeclaration e ? e : null;
    }

    private boolean isUnion(SchemaType type) {
        return type instanceof Reference reference && declared.get(reference.target()) instanceof UnionDeclaration;
    }

    private static SchemaType elementOf(SchemaType type) {
        if (type instanceof Vector vector) {
            return vector.element();
        }
        return type instanceof FixedArray array ? array.element() : type;
    }

    /** A declared table named {@code name}, simple or qualified. */
    private TableDeclaration findTable(String name) {
        for (Declaration declaration : declared.values()) {
            if (declaration instanceof TableDeclaration table
                    && (table.name().qualified().equals(name) || table.name().name().equals(name))) {
                return table;
            }
        }
        return null;
    }
}
