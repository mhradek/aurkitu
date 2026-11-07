package io.github.mhradek.aurkitu.plugin.core;

import io.github.mhradek.aurkitu.plugin.core.model.Declaration;
import io.github.mhradek.aurkitu.plugin.core.model.EnumDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.Field;
import io.github.mhradek.aurkitu.plugin.core.model.FieldAttribute;
import io.github.mhradek.aurkitu.plugin.core.model.ScalarKind;
import io.github.mhradek.aurkitu.plugin.core.model.Schema;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.FixedArray;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Reference;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Scalar;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.StringType;
import io.github.mhradek.aurkitu.plugin.core.model.SchemaType.Vector;
import io.github.mhradek.aurkitu.plugin.core.model.StructDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TableDeclaration;
import io.github.mhradek.aurkitu.plugin.core.model.TypeName;
import io.github.mhradek.aurkitu.plugin.core.model.UnionDeclaration;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the validator against flatc itself. Every field type, in a table and in a struct, gets every field
 * attribute alone and every pair of them (so attributes that affect one another are covered). For each case:
 * <ol>
 *   <li>the schema Aurkitu writes (after validation) is always accepted by flatc, and</li>
 *   <li>Aurkitu reports an issue exactly when flatc rejects the schema as modelled (before validation):
 *       no missed problems, no false alarms, except the deliberate differences in {@link #STRICTER}.</li>
 * </ol>
 * Needs flatc (see {@link FlatcValidationTest#findFlatc()}); required on CI.
 */
class FlatcAgreementTest {

    private static final TypeName ROOT = name("Root");
    private static final TypeName STRUCT = name("S");
    private static final Scalar INT = new Scalar(ScalarKind.INT);

    /** Field types, with a default value of the right form for each. */
    private static final Map<String, SchemaType> TYPES = Map.of(
            "int", INT,
            "bool", new Scalar(ScalarKind.BOOL),
            "float", new Scalar(ScalarKind.FLOAT),
            "string", StringType.INSTANCE,
            "bytes", new Vector(new Scalar(ScalarKind.UBYTE)),
            "ints", new Vector(INT),
            "enum", new Reference(name("Color")),
            "table", new Reference(name("Other")),
            "struct", new Reference(name("Vec")),
            "union", new Reference(name("Shape")));

    private static final Map<String, String> DEFAULTS = Map.of(
            "int", "1", "bool", "true", "float", "1.5", "enum", "GREEN");

    /**
     * Where Aurkitu reports an issue although flatc accepts the schema, on purpose. Keyed by a substring of the issue.
     */
    private static final List<String> STRICTER = List.of();

    @TempDir
    Path tempDir;

    /** One attribute (or option) applied to a field. */
    record Option(String label, Function<Field, Field> apply) {
    }

    record Case(String label, Schema schema) {
    }

    record Outcome(Case testCase, List<String> issues, Optional<String> rawRejection, Optional<String> cleanedRejection) {
    }

    @Test
    void validatorAgreesWithFlatc() throws Exception {
        Optional<String> flatc = FlatcValidationTest.findFlatc();
        if (flatc.isEmpty()) {
            assertNotEquals("true", System.getenv("CI"), "flatc must be available on CI");
            Assumptions.abort("flatc not found");
        }
        List<Case> cases = cases();
        List<Outcome> outcomes = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < cases.size(); i++) {
                Case testCase = cases.get(i);
                Path dir = tempDir.resolve("case" + i);
                futures.add(pool.submit(() -> {
                    SchemaValidator.Result result = new SchemaValidator().validate(testCase.schema());
                    outcomes.add(new Outcome(testCase, result.issues(),
                            compile(flatc.get(), dir.resolve("raw"), testCase.schema()),
                            compile(flatc.get(), dir.resolve("cleaned"), result.schema())));
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            pool.shutdown();
        }

        // One line per disagreement, grouped by kind, so the whole list fits in a CI log.
        List<String> problems = new ArrayList<>();
        for (Outcome o : outcomes) {
            String label = o.testCase().label();
            o.cleanedRejection().ifPresent(error -> problems.add("WRITTEN-REJECTED | " + label + " | flatc: " + error
                    + " | issues: " + o.issues()));
            boolean reported = o.issues().stream().anyMatch(issue -> STRICTER.stream().noneMatch(issue::contains));
            if (o.rawRejection().isPresent() && o.issues().isEmpty()) {
                problems.add("MISSED | " + label + " | flatc: " + o.rawRejection().get());
            } else if (o.rawRejection().isEmpty() && reported) {
                problems.add("FALSE-ALARM | " + label + " | issues: " + o.issues());
            }
        }
        Collections.sort(problems);
        assertTrue(problems.isEmpty(), problems.size() + " disagreements in " + outcomes.size() + " cases:\n"
                + String.join("\n", problems));
    }

    /** Every field type in a table and in a struct, with no attribute, each attribute, and each pair. */
    static List<Case> cases() {
        List<Option> options = options();
        List<Case> cases = new ArrayList<>();
        for (Map.Entry<String, SchemaType> type : new java.util.TreeMap<>(TYPES).entrySet()) {
            for (boolean inStruct : new boolean[]{false, true}) {
                cases.add(make(type.getKey(), type.getValue(), inStruct, List.of()));
                for (int a = 0; a < options.size(); a++) {
                    cases.add(make(type.getKey(), type.getValue(), inStruct, List.of(options.get(a))));
                    for (int b = a + 1; b < options.size(); b++) {
                        cases.add(make(type.getKey(), type.getValue(), inStruct, List.of(options.get(a), options.get(b))));
                    }
                }
            }
        }
        return cases;
    }

    private static List<Option> options() {
        return List.of(
                attribute("required", new FieldAttribute.Required()),
                attribute("deprecated", new FieldAttribute.Deprecated()),
                attribute("key", new FieldAttribute.Key()),
                attribute("hash", new FieldAttribute.Hash("fnv1_32")),
                attribute("nested_flatbuffer", new FieldAttribute.NestedFlatbuffer("Other")),
                attribute("flexbuffer", new FieldAttribute.Flexbuffer()),
                attribute("force_align", new FieldAttribute.ForceAlign(16)),
                attribute("custom", new FieldAttribute.Custom("priority", "1")),
                // A union field takes two ids (the hidden _type field and its own), so its only id is 1.
                new Option("id", f -> f.withAttributes(append(f.attributes(),
                        new FieldAttribute.Id(f.type().equals(TYPES.get("union")) ? 1 : 0)))),
                new Option("default", f -> f.withDefaultValue(DEFAULTS.getOrDefault(typeKey(f.type()), "1"))),
                new Option("fixedLength", f -> f.withType(f.type() instanceof Vector v ? new FixedArray(v.element(), 2)
                        : new FixedArray(f.type(), 2))));
    }

    private static Option attribute(String label, FieldAttribute attribute) {
        return new Option(label, f -> f.withAttributes(append(f.attributes(), attribute)));
    }

    private static List<FieldAttribute> append(List<FieldAttribute> attributes, FieldAttribute attribute) {
        List<FieldAttribute> all = new ArrayList<>(attributes);
        all.add(attribute);
        return all;
    }

    private static String typeKey(SchemaType type) {
        return TYPES.entrySet().stream().filter(e -> e.getValue().equals(type)).map(Map.Entry::getKey).findFirst().orElse("");
    }

    private static Case make(String typeLabel, SchemaType type, boolean inStruct, List<Option> applied) {
        Field field = new Field("f", "f", type, "", "", List.of());
        for (Option option : applied) {
            field = option.apply().apply(field);
        }
        List<Declaration> declarations = new ArrayList<>(List.of(
                new EnumDeclaration(name("Color"), "Color", "", ScalarKind.BYTE,
                        List.of(new EnumDeclaration.Value("RED", null), new EnumDeclaration.Value("GREEN", null)), null),
                new StructDeclaration(name("Vec"), "Vec", "", List.of(Field.of("x", new Scalar(ScalarKind.FLOAT))), false),
                new TableDeclaration(name("Other"), "Other", "", List.of(Field.of("v", INT)), false),
                UnionDeclaration.of(name("Shape"), "Shape", "", List.of("Other"))));
        if (inStruct) {
            declarations.add(new StructDeclaration(STRUCT, "S", "", List.of(field), false));
            declarations.add(new TableDeclaration(ROOT, "Root", "", List.of(Field.of("s", new Reference(STRUCT))), true));
        } else {
            declarations.add(new TableDeclaration(ROOT, "Root", "", List.of(field), true));
        }
        String label = typeLabel + " in " + (inStruct ? "struct" : "table") + " with "
                + (applied.isEmpty() ? "nothing" : String.join(" + ", applied.stream().map(Option::label).toList()));
        return new Case(label, new Schema("app", List.of(), null, null, declarations));
    }

    /** Compiles the schema with flatc for Java and C++; the error output if it is rejected. */
    private static Optional<String> compile(String flatc, Path dir, Schema schema) throws Exception {
        Files.createDirectories(dir);
        Path file = dir.resolve("case.fbs");
        Files.writeString(file, schema.write("// test", false, List.of()));
        Process process = new ProcessBuilder(flatc, "--java", "--cpp", "-o", dir.resolve("out").toString(), file.toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return process.waitFor() == 0 ? Optional.empty()
                : Optional.of(output.strip().replace(file.toString(), "case.fbs").replace('\n', ' ').replaceAll("\\s+", " "));
    }

    private static TypeName name(String n) {
        return new TypeName("app", n);
    }

    @Test
    void casesCoverEveryTypeAttributeAndPair() {
        // 10 types x 2 places x (1 + 11 + 55)
        List<Case> cases = cases();
        assertEquals(10 * 2 * (1 + 11 + 55), cases.size());
        assertEquals(cases.size(), cases.stream().map(Case::label).distinct().count());
        for (Case c : cases) {
            SchemaValidator.Result result = assertDoesNotThrow(() -> new SchemaValidator().validate(c.schema()), c.label());
            assertDoesNotThrow(() -> result.schema().write("// test", false, result.issues()), c.label());
        }
        // The baseline (no attributes) is valid wherever the type is allowed.
        assertEquals(List.of(), new SchemaValidator().validate(cases.get(0).schema()).issues(), cases.get(0).label());
    }
}
