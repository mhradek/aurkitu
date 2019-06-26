#!/bin/sh

# Build and test convenience script
mvn clean install -T 1C

# Kill script if anything goes wrong in the previous step
if [[ "$?" -ne 0 ]] ; then
  echo 'Build and/or tests failed'; exit $rc
fi

# Run the integration tests
cd aurkitu-test-service/ && mvn clean install -f pom-test.xml -o

#echo "Compiling schemas to java"
#target/bin/flatc --java --gen-mutable -o target/aurkitu/output/java target/aurkitu/schemas/*.fbs

#echo "Compiling schemas to cpp"
#target/bin/flatc --cpp -o target/aurkitu/output/cpp target/aurkitu/schemas/*.fbs
