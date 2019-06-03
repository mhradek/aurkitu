#!/bin/sh

# Build and test convenience script
mvn clean install

# Kill script if anything goes wrong in the previous step
if [[ "$?" -ne 0 ]] ; then
  echo 'Build and/or tests failed'; exit $rc
fi

# Run the integration tests
cd aurkitu-test-service/ && mvn clean install -f pom-test.xml -X

