package io.github.mhradek.aurkitu.plugin;

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test: the Mojo must scan the consuming project's compile classpath,
 * not the plugin's own classpath. The models here are compiled at test time into
 * a temp directory that is NOT on the test classpath, mirroring a real build where
 * the project's target/classes is invisible to the plugin's classloader.
 */
class AurkituMojoProjectClasspathTest {

    @TempDir
    Path tempDir;

    @Test
    void scansClassesFromProjectCompileClasspath() throws Exception {
        Path src = tempDir.resolve("src/e2e/models");
        Path classes = tempDir.resolve("classes");
        Files.createDirectories(src);
        Files.createDirectories(classes);

        Path user = src.resolve("ProjectUser.java");
        Files.writeString(user, """
                package e2e.models;
                import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
                @FlatBufferTable(rootType = true)
                public class ProjectUser {
                    private long id;
                    private String displayName;
                    private ProjectColor color;
                }
                """);
        Path color = src.resolve("ProjectColor.java");
        Files.writeString(color, """
                package e2e.models;
                import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;
                @FlatBufferEnum
                public enum ProjectColor { RED, GREEN }
                """);

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "Tests require a JDK");
        int rc = javac.run(null, null, null,
                "-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString(),
                user.toString(), color.toString());
        assertEquals(0, rc, "Test model compilation failed");

        // Sanity check: the models must not be visible to the test/plugin classloader.
        assertThrows(ClassNotFoundException.class, () -> Class.forName("e2e.models.ProjectUser"));

        MavenProject project = new MavenProject();
        Build build = new Build();
        build.setOutputDirectory(classes.toString());
        project.setBuild(build);

        Path out = tempDir.resolve("out");
        AurkituMojo mojo = new AurkituMojo();
        set(mojo, "project", project);
        set(mojo, "schemaName", "project");
        set(mojo, "schemaNamespace", "e2e.fb");
        set(mojo, "outputDirectory", out.toFile());
        set(mojo, "specifiedPackages", List.of("e2e.models"));
        set(mojo, "validate", true);
        set(mojo, "ignoreStaticMembers", true);
        set(mojo, "mapCommonTypes", true);
        set(mojo, "snakeCaseFields", true);

        mojo.execute();

        String schema = Files.readString(out.resolve("project.fbs"));
        assertTrue(schema.endsWith("""
                namespace e2e.fb;

                enum ProjectColor : byte {
                  RED,
                  GREEN,
                }

                table ProjectUser {
                  id:long;
                  display_name:string;
                  color:ProjectColor;
                }

                root_type ProjectUser;
                """), schema);
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
