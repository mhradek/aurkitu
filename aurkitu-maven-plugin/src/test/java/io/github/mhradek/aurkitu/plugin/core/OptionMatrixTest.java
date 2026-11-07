package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.AfterAll;
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
 * Every generator option, on and off, in every combination (2^11 = 2048), against the {@code options}
 * models. For each combination the exact expected schema is built from the options alone ({@link #expected}),
 * so an option that changes the output of another fails here. Also checked per combination: failing on
 * validation issues, the cache keeping an unchanged file, and the version hash depending only on the schema.
 */
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
                   boolean fileIdentifier, boolean includes, boolean fileExtension) {

        static Options of(int bits) {
            return new Options(bit(bits, 0), bit(bits, 1), bit(bits, 2), bit(bits, 3), bit(bits, 4), bit(bits, 5),
                    bit(bits, 6), bit(bits, 7), bit(bits, 8), bit(bits, 9), bit(bits, 10));
        }

        private static boolean bit(int bits, int index) {
            return (bits & (1 << index)) != 0;
        }

        SchemaGenerator.Config config() {
            return new SchemaGenerator.Config(null, List.of(PACKAGE), validate, ignoreStatic,
                    fileIdentifier ? "OPTS" : null, includes ? List.of("base.fbs") : List.of(), mapCommonTypes, cache,
                    snakeCase, version, failOnIssues, fileExtension ? "dat" : null,
                    // Scanning only the test classes keeps 2048 generations fast; the output is the same.
                    List.of(TEST_CLASSES), optionalScalars);
        }

        int issueCount() {
            return mapCommonTypes ? 1 : 2;
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
        return IntStream.range(0, 1 << 11).mapToObj(Options::of);
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
        assertEquals(1 << 7, HASHES.size(), "schema bodies seen with a version");
        assertEquals(HASHES.size(), distinct.size(), "version hashes collide");
    }

    /** Generates, and checks that it fails exactly when validation is on, failOnValidationIssues is set and there are issues. */
    private void generate(SchemaGenerator generator, Options options) throws Exception {
        if (options.fails()) {
            SchemaGenerationException e = assertThrows(SchemaGenerationException.class,
                    () -> generator.generate("opts", "opts", tempDir.toFile()), options.toString());
            assertEquals("Schema validation failed with " + options.issueCount() + " issue(s). See schema comments.", e.getMessage());
        } else {
            generator.generate("opts", "opts", tempDir.toFile());
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
            out.append("// - Unsupported field type: Account.nickname\n\n// Warnings:\n").append(INHERITED_WARNING);
        }
        return out.toString();
    }

    /** The schema itself: what the version hash covers. Depends on every option except validate, cache, failOnIssues and version. */
    static String body(Options o) {
        StringBuilder out = new StringBuilder();
        if (o.includes()) {
            out.append("include \"base.fbs\";\n\n");
        }
        out.append("namespace opts;\n\n");
        if (o.fileIdentifier()) {
            out.append("file_identifier \"OPTS\";\n\n");
        }
        if (o.fileExtension()) {
            out.append("file_extension \"dat\";\n\n");
        }
        out.append("struct Point {\n  x:int;\n  y:int;\n}\n\n");
        out.append("table Account {\n");
        out.append("  ").append(name(o, "user_id", "userId")).append(":long;\n");
        if (o.mapCommonTypes()) {
            out.append("  ").append(name(o, "created_at", "createdAt")).append(":string;\n");
        }
        out.append("  ").append(name(o, "login_count", "loginCount")).append(":int").append(o.optionalScalars() ? " = null" : "").append(";\n");
        out.append("  ").append(name(o, "retry_limit", "retryLimit")).append(":int = 3;\n");
        out.append("  ").append(name(o, "display_name", "displayName")).append(":string (required);\n");
        String mapTable = "MapValueSet_Account_" + name(o, "score_totals", "scoreTotals");
        out.append("  ").append(name(o, "score_totals", "scoreTotals")).append(":[").append(mapTable).append("];\n");
        out.append("  home:Point;\n");
        if (!o.ignoreStatic()) {
            out.append("  ").append(name(o, "instance_count", "instanceCount")).append(":int;\n");
        }
        out.append("  ").append(name(o, "tenant_id", "tenantId")).append(":long;\n}\n\n");
        out.append("table ").append(mapTable).append(" {\n  key:string;\n  value:int;\n}\n\n");
        out.append("root_type Account;\n");
        return out.toString();
    }

    private static String name(Options o, String snake, String camel) {
        return o.snakeCase() ? snake : camel;
    }
}
