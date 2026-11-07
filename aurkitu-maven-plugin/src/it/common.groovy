// Shared helpers for invoker verify scripts. Usage:
//   def it = evaluate(new File(itSourceDir, 'common.groovy'))
//   it.assertSchemaMatches(basedir, 'name')

/** Replace values that legitimately change between builds. */
def normalise = { String text ->
    text.replace('\r\n', '\n')
        .replaceFirst(/^\/\/ Aurkitu \S+ /, '// Aurkitu @VERSION@ ')
}

/** Compare target/aurkitu/schemas/<name>.fbs with the checked-in expected/<name>.fbs. */
def assertSchemaMatches = { File projectDir, String name ->
    File actual = new File(projectDir, "target/aurkitu/schemas/${name}.fbs")
    File expected = new File(projectDir, "expected/${name}.fbs")
    assert actual.file : "Schema not generated: ${actual}"
    assert expected.file : "Missing expected schema: ${expected}"
    String a = normalise(actual.text)
    String e = normalise(expected.text)
    if (a != e) {
        List<String> al = a.readLines(), el = e.readLines()
        Integer line = (0..<Math.max(al.size(), el.size())).find { i -> al[i] != el[i] }
        if (line == null) {
            throw new AssertionError("Generated ${name}.fbs differs from expected/${name}.fbs only in trailing newlines")
        }
        throw new AssertionError("""Generated ${name}.fbs differs from expected/${name}.fbs at line ${line + 1}:
  expected: ${el[line]}
  actual:   ${al[line]}
---- full generated schema ----
${a}""")
    }
    return actual
}

/**
 * Compile the schema with flatc when FLATC_PATH is set. Skipped locally without it; required in CI
 * (CI=true), so the check cannot silently stop running there.
 */
def runFlatc = { File projectDir, File schema, List<String> languages ->
    String flatc = System.getenv('FLATC_PATH')
    if (!flatc) {
        assert System.getenv('CI') != 'true' : 'FLATC_PATH must be set in CI so generated schemas are compiled with flatc'
        return false
    }
    File out = new File(projectDir, 'target/flatc-out')
    List<String> cmd = [flatc] + languages + ['-I', projectDir.absolutePath, '-o', out.absolutePath, schema.absolutePath]
    def proc = cmd.execute()
    def stdout = new StringBuilder(), stderr = new StringBuilder()
    proc.waitForProcessOutput(stdout, stderr)
    assert proc.exitValue() == 0 : "flatc rejected ${schema.name}:\n${stdout}${stderr}"
    return true
}

return [assertSchemaMatches: assertSchemaMatches, runFlatc: runFlatc, normalise: normalise]
