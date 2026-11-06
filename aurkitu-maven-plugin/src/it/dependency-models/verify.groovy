def it = evaluate(new File(itSourceDir, 'common.groovy'))

// The models module is a plain jar; the plugin runs in service and must find the jar's types.
assert new File(basedir, 'models/target/models-1.0-SNAPSHOT.jar').file : 'models jar was not built'

File serviceDir = new File(basedir, 'service')
File schema = it.assertSchemaMatches(serviceDir, 'service')
it.runFlatc(serviceDir, schema, ['--cpp', '--java'])
return true
