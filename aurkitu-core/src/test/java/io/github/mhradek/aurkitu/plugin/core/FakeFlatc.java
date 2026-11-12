package io.github.mhradek.aurkitu.plugin.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Stand-ins for {@code flatc} in tests, so no binary is needed: a shell script, or on Windows a {@code .cmd} file,
 * which Java runs the same way. Every test that runs flatc then runs on every OS, so the coverage gate holds
 * everywhere. (Shared with the Maven plugin's tests through the core's test jar.)
 */
public final class FakeFlatc {

    /** True on Windows, where the stand-ins are {@code .cmd} files. */
    public static final boolean WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

    private FakeFlatc() {}

    /** Writes its arguments, space-separated, to {@code argsFile}, and succeeds. */
    public static Path recordingArguments(Path dir, Path argsFile) throws IOException {
        return write(dir, "flatc-ok",
                "echo \"$@\" > '" + argsFile + "'\nexit 0\n",
                ">\"" + argsFile + "\" echo %*\r\nexit /b 0\r\n");
    }

    /** Prints {@code message} and fails, as flatc does when it rejects a schema. */
    public static Path rejecting(Path dir, String message) throws IOException {
        return write(dir, "flatc-reject",
                "echo '" + message + "'\nexit 1\n",
                "echo " + message + "\r\nexit /b 1\r\n");
    }

    /** Creates {@code marker} and succeeds, to show that it ran. */
    public static Path touching(Path dir, Path marker) throws IOException {
        return write(dir, "flatc-record",
                "touch '" + marker + "'\nexit 0\n",
                "type nul > \"" + marker + "\"\r\nexit /b 0\r\n");
    }

    private static Path write(Path dir, String name, String posixBody, String windowsBody) throws IOException {
        if (WINDOWS) {
            return Files.writeString(dir.resolve(name + ".cmd"), "@echo off\r\n" + windowsBody);
        }
        Path script = Files.writeString(dir.resolve(name), "#!/bin/sh\n" + posixBody);
        if (!script.toFile().setExecutable(true)) {
            throw new IOException("Could not make " + script + " executable");
        }
        return script;
    }
}
