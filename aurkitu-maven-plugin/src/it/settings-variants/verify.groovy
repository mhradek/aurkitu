def it = evaluate(new File(itSourceDir, 'common.groovy'))

File schema = it.assertSchemaMatches(basedir, 'variants')
it.runFlatc(basedir, schema, ['--cpp', '--java'])

String log = new File(basedir, 'build.log').text
assert log.contains('Validation issues found (3)') : 'Expected three validation warnings'
assert log.contains('BUILD SUCCESS') : 'Validation issues must not fail the build by default'
return true
