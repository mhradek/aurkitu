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
    assert actual.isFile() : "Schema not generated: ${actual}"
    assert expected.isFile() : "Missing expected schema: ${expected}"
    String a = normalise(actual.text)
    String e = normalise(expected.text)
    if (a != e) {
        List<String> al = a.readLines(), el = e.readLines()
        int line = (0..<Math.max(al.size(), el.size())).find { i -> al[i] != el[i] }
        throw new AssertionError("""Generated ${name}.fbs differs from expected/${name}.fbs at line ${line + 1}:
  expected: ${el[line]}
  actual:   ${al[line]}
---- full generated schema ----
${a}""")
    }
    return actual
}

/** Compile the schema with flatc when FLATC_PATH is set (CI); skipped otherwise. */
def runFlatc = { File projectDir, File schema, List<String> languages ->
    String flatc = System.getenv('FLATC_PATH')
    if (!flatc) {
        println "FLATC_PATH not set; skipping flatc check of ${schema.name}"
        return
    }
    File out = new File(projectDir, 'target/flatc-out')
    List<String> cmd = [flatc] + languages + ['-I', projectDir.absolutePath, '-o', out.absolutePath, schema.absolutePath]
    def proc = cmd.execute()
    def stdout = new StringBuilder(), stderr = new StringBuilder()
    proc.waitForProcessOutput(stdout, stderr)
    assert proc.exitValue() == 0 : "flatc rejected ${schema.name}:\n${stdout}${stderr}"
    println "flatc ${languages.join(' ')} accepted ${schema.name}"
}

return [assertSchemaMatches: assertSchemaMatches, runFlatc: runFlatc, normalise: normalise]
