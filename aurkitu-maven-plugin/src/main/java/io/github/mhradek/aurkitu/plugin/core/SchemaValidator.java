package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.CompositeDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.RpcServiceDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
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
import java.util.stream.Collectors;
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
 * dropped; a struct marked as root loses the mark
 * (flatc only accepts a table as root).</p>
 */
final class SchemaValidator {

    /**
     * @param issues   problems flatc would reject (or that lose information); they can fail the build
     * @param warnings risks to compatibility with existing data; reported, never fatal
     */
    record Result(Schema schema, List<String> issues, List<String> warnings) {}

    private static final Pattern NAMESPACE = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    Result validate(Schema schema) {
        // flatc rejects empty structs: one whose fields are all invalid is left out, with the fields that use it,
        // which can empty another struct, so validate until none is left empty.
        Set<TypeName> removed = new HashSet<>();
        while (true) {
            Result result = validate(schema, removed);
            Set<TypeName> empty = result.schema().declarations().stream()
                    .filter(d -> d instanceof StructDeclaration struct && struct.fields().isEmpty())
                    .map(Declaration::name).collect(Collectors.toSet());
            if (empty.isEmpty()) {
                return result;
            }
            removed.addAll(empty);
        }
    }

    private Result validate(Schema schema, Set<TypeName> removed) {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<TypeName, Declaration> declared = declarations(schema, issues);
        declared.keySet().removeAll(removed);

        String id = schema.fileIdentifier();
        if (id != null && !id.isBlank() && id.length() != 4) {
            issues.add("file_identifier \"" + id + "\" must be exactly 4 characters");
        }

        checkNamespaces(schema, issues);
        boolean checkReferences = schema.includes().isEmpty(); // included types cannot be resolved here
        CompositeValidator composites = new CompositeValidator(declared, removed, checkReferences, issues, warnings);

        List<Declaration> valid = new ArrayList<>();
        boolean rootSeen = false;
        for (Declaration declaration : schema.declarations()) {
            if (declaration instanceof StructDeclaration struct && struct.root()) {
                issues.add("Root type " + struct.name().name() + " is a struct; root_type must be a table");
                declaration = new StructDeclaration(struct.name(), struct.source(), struct.comment(), struct.fields(), false);
                rootSeen = true; // reported above; not also "No root type defined"
            }
            if (declaration instanceof CompositeDeclaration composite) {
                CompositeDeclaration checked = composites.validate(composite);
                if (removed.contains(checked.name())) {
                    issues.add("Struct " + checked.name().name() + " has no valid fields and is left out (flatc does not"
                            + " allow empty structs)");
                    continue;
                }
                valid.add(checked);
                if (composite.root()) {
                    if (rootSeen) {
                        issues.add("Multiple root types; last used: " + composite.name().name());
                    }
                    rootSeen = true;
                }
            } else if (declaration instanceof EnumDeclaration enumDeclaration) {
                valid.add(validate(enumDeclaration, issues));
            } else if (declaration instanceof UnionDeclaration union) {
                valid.add(validate(union, declared, checkReferences, issues));
            } else if (declaration instanceof RpcServiceDeclaration service) {
                valid.add(validate(service, declared, checkReferences, issues));
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
    private static UnionDeclaration validate(UnionDeclaration union, Map<TypeName, Declaration> declared,
                                             boolean checkReferences, List<String> issues) {
        String name = union.name().name();
        if (union.members().isEmpty()) {
            issues.add("Union " + name + " has no members");
        }
        Set<String> labels = new HashSet<>();
        for (UnionDeclaration.Member member : union.members()) {
            String label = member.label();
            if ("NONE".equals(label)) {
                issues.add("Union " + name + " member NONE: the name is reserved (flatc adds NONE for no value)");
            } else if (!labels.add(label)) {
                issues.add("Union " + name + " has two members named " + label);
            }
            Declaration target = declared.get(member.type());
            if (target == null && checkReferences) {
                issues.add("Union " + name + " member " + label + " is not declared in this schema");
            } else if (target instanceof EnumDeclaration || target instanceof UnionDeclaration
                    || target instanceof RpcServiceDeclaration) {
                issues.add("Union " + name + " member " + label + " is not a table");
            }
        }
        return union.withAttributes(CompositeValidator.validCustom(union.attributes(), "union " + name, issues));
    }

    /**
     * RPC methods take one table and return one; methods with another signature are dropped. Request
     * and response types must be declared tables (unless they may come from an include).
     */
    private static RpcServiceDeclaration validate(RpcServiceDeclaration service, Map<TypeName, Declaration> declared,
                                                  boolean checkReferences, List<String> issues) {
        String name = service.name().name();
        List<RpcServiceDeclaration.Method> valid = new ArrayList<>();
        for (RpcServiceDeclaration.Method method : service.methods()) {
            String where = name + "." + method.name();
            if (!method.valid()) {
                issues.add("RPC method " + where + " skipped: it must take exactly one table and return a table");
                continue;
            }
            for (TypeName type : List.of(method.request(), method.response())) {
                Declaration target = declared.get(type);
                if (target == null && checkReferences) {
                    issues.add("RPC method " + where + " uses " + type.qualified() + ", which is not declared in this schema");
                } else if (target != null && !(target instanceof TableDeclaration)) {
                    issues.add("RPC method " + where + " uses " + type.qualified() + ", which is not a table");
                }
            }
            valid.add(method);
        }
        if (valid.isEmpty()) {
            issues.add("RPC service " + name + " has no methods");
        }
        return service.withMethods(valid);
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

    private static EnumDeclaration validate(EnumDeclaration enumDeclaration, List<String> issues) {
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
        List<FieldAttribute> attributes = CompositeValidator.validCustom(enumDeclaration.attributes(), "enum " + name, issues);
        if (enumDeclaration.bitFlags() && !bitFlagsValid(enumDeclaration, issues)) {
            attributes = attributes.stream().filter(a -> !(a instanceof FieldAttribute.BitFlags)).toList();
        }
        return enumDeclaration.withAttributes(attributes);
    }

    /** bit_flags: an unsigned type, and each value (explicit, or its position) a bit of that width. */
    private static boolean bitFlagsValid(EnumDeclaration enumDeclaration, List<String> issues) {
        String name = enumDeclaration.name().name();
        ScalarKind kind = enumDeclaration.underlying();
        if (!kind.isUnsigned()) {
            issues.add("bit_flags on enum " + name + " skipped: needs an unsigned enumType (UBYTE, USHORT, UINT, ULONG)");
            return false;
        }
        int bits = kind.bits();
        List<EnumDeclaration.Value> values = enumDeclaration.values();
        for (int i = 0; i < values.size(); i++) {
            long position = values.get(i).value() != null ? values.get(i).value() : i;
            if (position < 0 || position >= bits) {
                issues.add("Enum " + name + " value " + values.get(i).name() + " is bit " + position
                        + ", but bit_flags on " + kind.keyword() + " allows bits 0 to " + (bits - 1));
            }
        }
        return true;
    }
}
