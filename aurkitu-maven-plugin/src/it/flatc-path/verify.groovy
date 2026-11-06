String log = new File(basedir, 'build.log').text
assert log.contains('flatc validation succeeded for checked.fbs')
return true
