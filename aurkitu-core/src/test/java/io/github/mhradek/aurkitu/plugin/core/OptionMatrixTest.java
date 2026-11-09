package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Every generator option, on and off, in every combination (2^14 = 16384), against the {@code options}
 * models. For each combination the exact expected schema is built from the options alone ({@link #expected}),
 * so an option that changes the output of another fails here. Also checked per combination: failing on
 * validation issues, the cache keeping an unchanged file, and the version hash depending only on the schema.
 */
@Execution(ExecutionMode.CONCURRENT) // each combination has its own directory
class OptionMatrixTest {

    private static final String PACKAGE = "io.github.mhradek.aurkitu.plugin.core.options";
    private static final String TEST_CLASSES = testClasses();
    private static final FileTime EPOCH = FileTime.fromMillis(0);
    private static final String INHERITED_WARNING = "// - Table Account has inherited fields (from AccountBase) after its own;"
            + " adding a field to Account moves them and breaks existing data. Give every field a fieldId to fix their positions.\n";

    /** Version hash seen for each distinct schema body (version line and comments excluded). */
    private static final Map<String, String> HASHES = new HashMap<>();

    @TempDir
    Path tempDir;

    record Options(boolean validate, boolean ignoreStatic, boolean mapCommonTypes, boolean cache, boolean snakeCase,
                   boolean version, boolean failOnIssues, boolean optionalScalars,
                   boolean fileIdentifier, boolean includes, boolean fileExtension, boolean auditNamespace,
                   boolean snakeCaseAcronyms, boolean typedMapKeys) {

        static final int COUNT = 14;

        static Options of(int bits) {
            return new Options(bit(bits, 0), bit(bits, 1), bit(bits, 2), bit(bits, 3), bit(bits, 4), bit(bits, 5),
                    bit(bits, 6), bit(bits, 7), bit(bits, 8), bit(bits, 9), bit(bits, 10), bit(bits, 11),
                    bit(bits, 12), bit(bits, 13));
        }

        /** schemaNamespace: "opts", or "audit", the namespace Audit declares for itself. */
        String namespace() {
            return auditNamespace ? "audit" : "opts";
        }

        private static boolean bit(int bits, int index) {
            return (bits & (1 << index)) != 0;
        }

        SchemaGenerator.Config config() {
            return new SchemaGenerator.Config(List.of(PACKAGE), validate, ignoreStatic,
                    fileIdentifier ? "OPTS" : null, includes ? List.of("base.fbs") : List.of(), mapCommonTypes, cache,
                    snakeCase, version, failOnIssues, fileExtension ? "dat" : null,
                    // Scanning only the test classes keeps the generations fast; the output is the same.
                    List.of(TEST_CLASSES), optionalScalars)
                    .withSnakeCaseAcronyms(snakeCaseAcronyms)
                    .withTypedMapKeys(typedMapKeys);
        }

        /** A Date map key needs mapCommonTypes once keys are typed. */
        boolean dateKeyUnsupported() {
            return typedMapKeys && !mapCommonTypes;
        }

        int issueCount() {
            return (mapCommonTypes ? 0 : 1) + 1 // createdAt, nickname
                    + (includes ? 0 : 1)        // ghost: undeclared, unless an include may declare it
                    + (snakeCase ? 1 : 0)       // nickName and nick_name clash
                    + (ignoreStatic ? 0 : 1)    // Ledger's static field has no id
                    + (dateKeyUnsupported() ? 1 : 0);
        }

        boolean fails() {
            return validate && failOnIssues;
        }
    }

    private static String testClasses() {
        try {
            return new File(OptionMatrixTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static Stream<Options> combinations() {
        return IntStream.range(0, 1 << Options.COUNT).mapToObj(Options::of);
    }

    @ParameterizedTest
    @MethodSource("combinations")
    void everyCombinationOfOptions(Options options) throws Exception {
        SchemaGenerator generator = new SchemaGenerator(options.config());
        Path schema = tempDir.resolve("opts.fbs");

        generate(generator, options);
        String content = Files.readString(schema);
        String withoutVersion = content;
        if (options.version()) {
            String line = content.lines().skip(1).findFirst().orElseThrow();
            assertTrue(line.matches("// @version: [0-9a-f]{1,8}"), line);
            withoutVersion = content.replace(line + "\n", "");
            synchronized (HASHES) {
                String previous = HASHES.putIfAbsent(body(options), line);
                assertTrue(previous == null || previous.equals(line), "same schema, different version: " + options);
            }
        }
        assertEquals(expected(options), withoutVersion, options.toString());

        // Second run on unchanged output: the cache keeps the file (and its timestamp); without it, it is rewritten.
        Files.setLastModifiedTime(schema, EPOCH);
        generate(generator, options);
        assertEquals(content, Files.readString(schema), options.toString());
        assertEquals(options.cache(), EPOCH.equals(Files.getLastModifiedTime(schema)), options.toString());
    }

    @AfterAll
    static void differentSchemasHaveDifferentVersions() {
        Set<String> distinct = new HashSet<>(HASHES.values());
        long bodies = combinations().map(OptionMatrixTest::body).distinct().count();
        assertEquals(bodies, HASHES.size(), "schema bodies seen with a version");
        assertEquals(HASHES.size(), distinct.size(), "version hashes collide");
    }

    /** Generates, and checks that it fails exactly when validation is on, failOnValidationIssues is set and there are issues. */
    private void generate(SchemaGenerator generator, Options options) throws Exception {
        if (options.fails()) {
            SchemaGenerationException e = assertThrows(SchemaGenerationException.class,
                    () -> generator.generate("opts", options.namespace(), tempDir.toFile()), options.toString());
            assertEquals("Schema validation failed with " + options.issueCount() + " issue(s). See schema comments.", e.getMessage());
        } else {
            generator.generate("opts", options.namespace(), tempDir.toFile());
        }
    }

    /** The whole expected schema, without its version line. */
    static String expected(Options o) {
        StringBuilder out = new StringBuilder(AurkituVersion.schemaHeader()).append('\n').append(body(o));
        if (o.validate()) {
            out.append("\n// Validation issues:\n");
            if (!o.mapCommonTypes()) {
                out.append("// - Unsupported field type: Account.createdAt\n");
            }
            out.append("// - Unsupported field type: Account.nickname\n");
            if (!o.includes()) {
                out.append("// - Field Account.ghost references ").append(o.namespace()).append(".Ghost, which is not declared")
                        .append(" in this schema; annotate it, or include the schema that declares it\n");
            }
            if (o.dateKeyUnsupported()) {
                out.append("// - Unsupported field type: Account.loginsByDay\n");
            }
            if (o.snakeCase()) { // name clashes are checked after a table's fields
                out.append("// - Fields Account.nickName and Account.nick_name are both written as nick_name; nick_name left out\n");
            }
            if (!o.ignoreStatic()) {
                out.append("// - Partial field ids on Ledger; ids omitted (all fields or none must have an id)\n");
            }
            out.append("\n// Warnings:\n").append(INHERITED_WARNING);
        }
        return out.toString();
    }

    /**
     * The schema itself: what the version hash covers. Depends on every option except validate, cache,
     * failOnIssues and version.
     */
    static String body(Options o) {
        return header(o) + "struct Point {\n  x:int;\n  y:int;\n}\n\n" + account(o)
                // A namespace statement only where the namespace changes.
                + when(!o.auditNamespace(), "namespace audit;\n\n") + "table Audit {\n  at:long;\n}\n\n"
                + when(!o.auditNamespace(), "namespace opts;\n\n")
                + ledger(o) + mapTables(o) + "root_type Account;\n";
    }

    private static String header(Options o) {
        return when(o.includes(), "include \"base.fbs\";\n\n") + "namespace " + o.namespace() + ";\n\n"
                + when(o.fileIdentifier(), "file_identifier \"OPTS\";\n\n")
                + when(o.fileExtension(), "file_extension \"dat\";\n\n");
    }

    private static String account(Options o) {
        return "table Account {\n"
                + field(o, "user_id", "userId", "long")
                + when(o.mapCommonTypes(), field(o, "created_at", "createdAt", "string"))
                + field(o, "login_count", "loginCount", o.optionalScalars() ? "int = null" : "int")
                + field(o, "retry_limit", "retryLimit", "int = 3")
                + field(o, "display_name", "displayName", "string (required)")
                + field(o, "score_totals", "scoreTotals", "[" + scoreTable(o) + "]")
                + "  home:Point;\n"
                + when(!o.ignoreStatic(), field(o, "instance_count", "instanceCount", "int"))
                + when(o.includes(), "  ghost:Ghost;\n") // may come from the include, so it stays
                + field(o, "nick_name", "nickName", "string")
                + when(!o.snakeCase(), "  nick_name:string;\n") // only distinct from nickName without snake_case
                + field(o, "updated_at", "updatedAt", "string") // fieldType = STRING, even without mapCommonTypes
                + "  audit:" + (o.auditNamespace() ? "Audit" : "audit.Audit") + ";\n"
                + field(o, o.snakeCaseAcronyms() ? "account_url" : "account_u_r_l", "accountURL", "string")
                + "  " + namesField(o) + ":[MapValueSet_Account_" + namesField(o) + "];\n"
                + when(!o.dateKeyUnsupported(), field(o, "logins_by_day", "loginsByDay", "[" + loginsTable(o) + "]"))
                + field(o, "tenant_id", "tenantId", "long")
                + "}\n\n";
    }

    private static String ledger(Options o) {
        // ignoreStaticMembers=false adds a static field without an id, so the ids become partial and are left out.
        return "table Ledger {\n"
                + field(o, "entry_id", "entryId", o.ignoreStatic() ? "long (id: 0)" : "long")
                + (o.ignoreStatic() ? "  amount:int (id: 1);\n" : "  amount:int;\n")
                + when(!o.ignoreStatic(), field(o, "open_count", "openCount", "int"))
                + "}\n\n";
    }

    private static String mapTables(Options o) {
        String stringKey = o.typedMapKeys() ? "  key:string (key);\n" : "  key:string;\n";
        return "table " + scoreTable(o) + " {\n" + stringKey + "  value:int;\n}\n\n"
                + "table MapValueSet_Account_" + namesField(o) + " {\n"
                + (o.typedMapKeys() ? "  key:int (key);\n" : "  key:string;\n") + "  value:string;\n}\n\n"
                + when(!o.dateKeyUnsupported(), "table " + loginsTable(o) + " {\n" + stringKey + "  value:int;\n}\n\n");
    }

    private static String namesField(Options o) {
        return name(o, o.snakeCaseAcronyms() ? "names_by_id" : "names_by_i_d", "namesByID");
    }

    private static String loginsTable(Options o) {
        return "MapValueSet_Account_" + name(o, "logins_by_day", "loginsByDay");
    }

    private static String scoreTable(Options o) {
        return "MapValueSet_Account_" + name(o, "score_totals", "scoreTotals");
    }

    private static String when(boolean condition, String text) {
        return condition ? text : "";
    }

    private static String field(Options o, String snake, String camel, String type) {
        return "  " + name(o, snake, camel) + ":" + type + ";\n";
    }

    private static String name(Options o, String snake, String camel) {
        return o.snakeCase() ? snake : camel;
    }
}
