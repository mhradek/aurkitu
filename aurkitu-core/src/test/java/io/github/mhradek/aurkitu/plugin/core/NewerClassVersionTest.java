package io.github.mhradek.aurkitu.plugin.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model classes compiled for a newer Java than the JVM running the build, e.g. a Gradle toolchain targeting
 * Java 25 while Gradle runs on 17. Enum values need the class loaded, which that JVM cannot do; the schema must
 * not silently get other values.
 */
class NewerClassVersionTest {

    @TempDir
    Path tempDir;

    @Test
    void anEnumWithExplicitValuesCompiledForANewerJavaFailsWithTheReason() throws Exception {
        Path classes = compile("""
                package newer;

                import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
                import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;
                import io.github.mhradek.aurkitu.annotations.types.EnumType;

                @FlatBufferEnum(enumType = EnumType.SHORT)
                public enum Priority {
                    HIGH((short) 30), LOW((short) 10);

                    @FlatBufferEnumTypeField
                    private final short weight;

                    Priority(short weight) {
                        this.weight = weight;
                    }
                }
                """);
        int newer = Runtime.version().feature() + 1;
        stampClassVersion(classes.resolve("newer/Priority.class"), newer);

        SchemaGenerator.Config config = new SchemaGenerator.Config(List.of("newer"), true, true, null, List.of(),
                true, false, true, false, false, null, List.of(classes.toString()), false);
        SchemaGenerationException e = assertThrows(SchemaGenerationException.class,
                () -> new SchemaGenerator(config).generate("newer", "newer.fb", tempDir.toFile()));

        assertTrue(e.getMessage().contains("newer.Priority"), e.getMessage());
        assertTrue(e.getMessage().contains("Java " + newer), e.getMessage());
        assertTrue(e.getMessage().contains("Java " + Runtime.version().feature()), e.getMessage());
        assertFalse(Files.exists(tempDir.resolve("newer.fbs")), "no schema with wrong values is written");
    }

    /** Implicit values come from the class file alone, so no class needs loading and the schema is written. */
    @Test
    void anEnumWithImplicitValuesCompiledForANewerJavaIsStillWritten() throws Exception {
        Path classes = compile("""
                package newer;

                import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

                @FlatBufferEnum
                public enum Mood { HAPPY, SAD }
                """);
        stampClassVersion(classes.resolve("newer/Mood.class"), Runtime.version().feature() + 1);

        SchemaGenerator.Config config = new SchemaGenerator.Config(List.of("newer"), true, true, null, List.of(),
                true, false, true, false, false, null, List.of(classes.toString()), false);
        new SchemaGenerator(config).generate("newer", "newer.fb", tempDir.toFile());

        String schema = Files.readString(tempDir.resolve("newer.fbs"));
        assertTrue(schema.contains("enum Mood : byte {\n  HAPPY,\n  SAD,\n}"), schema);
    }

    /**
     * Any other reason the class cannot be loaded (here, an interface missing from the classpath) is not about the
     * build's Java version: it stays a validation issue, with implicit values, as before.
     */
    @Test
    void otherLoadFailuresStayAValidationIssue() throws Exception {
        Path classes = compile("""
                package newer;

                import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
                import io.github.mhradek.aurkitu.annotations.FlatBufferEnumTypeField;

                @FlatBufferEnum
                public enum Size implements Measured {
                    BIG(2), SMALL(1);

                    @FlatBufferEnumTypeField
                    private final int units;

                    Size(int units) {
                        this.units = units;
                    }
                }
                """, """
                package newer;

                public interface Measured {}
                """);
        Files.delete(classes.resolve("newer/Measured.class"));

        SchemaGenerator.Config config = new SchemaGenerator.Config(List.of("newer"), true, true, null, List.of(),
                true, false, true, false, false, null, List.of(classes.toString()), false);
        new SchemaGenerator(config).generate("newer", "newer.fb", tempDir.toFile());

        String schema = Files.readString(tempDir.resolve("newer.fbs"));
        assertTrue(schema.contains("enum Size : byte {\n  BIG,\n  SMALL,\n}"), schema);
        assertTrue(schema.contains("// - Enum Size: could not read @FlatBufferEnumTypeField units; values are implicit"), schema);
    }

    private Path compile(String... sources) throws Exception {
        Path src = Files.createDirectories(tempDir.resolve("src/newer"));
        List<String> args = new java.util.ArrayList<>();
        Path out = Files.createDirectories(tempDir.resolve("classes"));
        String annotations = new File(io.github.mhradek.aurkitu.annotations.FlatBufferEnum.class
                .getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        args.addAll(List.of("-d", out.toString(), "-cp", annotations));
        for (String source : sources) {
            String name = source.replaceAll("(?s).*?public (?:enum|interface) (\\w+).*", "$1");
            args.add(Files.writeString(src.resolve(name + ".java"), source).toString());
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertEquals(0, javac.run(null, null, null, args.toArray(new String[0])), "compiles");
        return out;
    }

    /** Rewrites the class file's major version, as if it had been compiled for Java {@code feature}. */
    private static void stampClassVersion(Path classFile, int feature) throws Exception {
        try (RandomAccessFile f = new RandomAccessFile(classFile.toFile(), "rw")) {
            f.seek(6);
            f.writeShort(44 + feature);
        }
    }
}
