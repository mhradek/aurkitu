package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import io.github.mhradek.aurkitu.plugin.core.model.StructDeclaration;
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
 * Checks a schema against the FlatBuffers rules (https://flatbuffers.dev/schema/). A pure
 * function: it returns a copy with everything that cannot be written removed, plus the issues
 * and warnings found, listed in schema order. Fields are checked by {@link CompositeValidator}.
 *
 * <p>Corrections: unsupported fields are dropped; invalid ids, attributes and defaults are
 * dropped; fixed-length arrays outside structs become vectors; non-scalar struct fields are
 * dropped when {@code dropInvalidStructFields} is set; a struct marked as root loses the mark
 * (flatc only accepts a table as root).</p>
 */
final class SchemaValidator {

    /**
     * @param issues   problems flatc would reject (or that lose information); they can fail the build
     * @param warnings risks to compatibility with existing data; reported, never fatal
     */
    record Result(Schema schema, List<String> issues, List<String> warnings) {}

    private static final Pattern NAMESPACE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private final boolean dropInvalidStructFields;

    SchemaValidator(boolean dropInvalidStructFields) {
        this.dropInvalidStructFields = dropInvalidStructFields;
    }

    Result validate(Schema schema) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<TypeName, Declaration> declared = declarations(schema, issues);

        String id = schema.fileIdentifier();
        if (id != null && !id.isBlank() && id.length() != 4) {
            issues.add("file_identifier \"" + id + "\" must be exactly 4 characters");
        }

        checkNamespaces(schema, issues);
        boolean checkReferences = schema.includes().isEmpty(); // included types cannot be resolved here
        CompositeValidator composites = new CompositeValidator(declared, checkReferences, dropInvalidStructFields, issues, warnings);

        List<Declaration> valid = new ArrayList<>();
        boolean rootSeen = false;
        for (Declaration declaration : schema.declarations()) {
            if (declaration instanceof StructDeclaration struct && struct.root()) {
                issues.add("Root type " + struct.name().name() + " is a struct; root_type must be a table");
                declaration = new StructDeclaration(struct.name(), struct.source(), struct.comment(), struct.fields(), false);
                rootSeen = true; // reported above; not also "No root type defined"
            }
            if (declaration instanceof CompositeDeclaration composite) {
                valid.add(composites.validate(composite));
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
        return new Result(schema.withDeclarations(valid), List.copyOf(issues), List.copyOf(warnings));
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
