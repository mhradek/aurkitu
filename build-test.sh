#!/bin/sh

# Build and test convenience script
mvn clean install
cd aurkitu-test-service/ && mvn clean install -f pom-test.xml

