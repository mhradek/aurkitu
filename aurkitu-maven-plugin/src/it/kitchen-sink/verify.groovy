def it = evaluate(new File(itSourceDir, 'common.groovy'))

File schema = it.assertSchemaMatches(basedir, 'kitchen')
it.runFlatc(basedir, schema, ['--cpp', '--java'])

String log = new File(basedir, 'build.log').text
assert log.contains('No issues found') : 'Expected a clean validation run'
return true
