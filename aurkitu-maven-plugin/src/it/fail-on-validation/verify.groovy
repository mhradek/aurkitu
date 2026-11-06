String log = new File(basedir, 'build.log').text
assert log.contains('BUILD FAILURE')
assert log.contains('Schema validation failed with 1 issue(s)') : 'Expected the validation failure message'
// A validation failure is a MojoFailureException ("build failure"), not a plugin error.
assert !log.contains('MojoExecutionException')

String schema = new File(basedir, 'target/aurkitu/schemas/failing.fbs').text
assert schema.contains('struct BadStruct {\n  ok:int;\n}') : "Invalid field must be dropped:\n${schema}"
assert schema.contains('// - Struct BadStruct field notAllowed is non-scalar')
return true
