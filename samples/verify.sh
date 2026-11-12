#!/usr/bin/env bash
# Builds the sample with Maven and with Gradle and checks that both write samples/game/expected/game.fbs (the
# version header aside); then, with flatc on the PATH or FLATC_PATH set, compiles it for Java and TypeScript.
#
#   samples/verify.sh                  with the released Aurkitu version the sample names (aurkituVersion)
#   samples/verify.sh --this-checkout  with this checkout's code: run `mvn install` at the root first
#
# CI runs it with --this-checkout, so the sample always matches what the code writes.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root/samples/game"

# The release checklist bumps the sample's version in both build files; they must agree.
maven_version=$(sed -n 's:.*<aurkituVersion>\(.*\)</aurkituVersion>.*:\1:p' pom.xml)
gradle_version=$(sed -n 's/^aurkituVersion=//p' gradle.properties)
if [ "$maven_version" != "$gradle_version" ]; then
    echo "aurkituVersion differs: $maven_version in pom.xml, $gradle_version in gradle.properties"
    exit 1
fi

# The version is always passed, so neither argument list is ever empty: bash 3.2 (macOS's /bin/bash) treats an
# empty "${array[@]}" as an unbound variable under set -u.
if [ "${1:-}" = "--this-checkout" ]; then
    version=$(cd "$root" && mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)
    echo "Building the sample against this checkout ($version)"
    maven_args=("-DaurkituVersion=$version")
    # Relative, so it also works from Git Bash on Windows, where an absolute /c/... path means nothing to Java.
    gradle_args=("-PaurkituVersion=$version" --include-build ../../aurkitu-gradle-plugin)
else
    version=$gradle_version
    echo "Building the sample against the released Aurkitu $version"
    maven_args=("-DaurkituVersion=$version")
    gradle_args=("-PaurkituVersion=$version")
fi

mvn -B -q clean package "${maven_args[@]}"
gradle -q clean build "${gradle_args[@]}"

status=0
for schema in target/aurkitu/schemas/game.fbs build/aurkitu/schemas/game.fbs; do
    # The first line names the Aurkitu version that wrote the schema; the rest must match exactly.
    if diff <(tail -n +2 expected/game.fbs) <(tail -n +2 "$schema"); then
        echo "OK: $schema matches expected/game.fbs"
    else
        echo "DIFFERENT: $schema (diff above). If the change is intended, update samples/game/expected/game.fbs."
        status=1
    fi
done

flatc=${FLATC_PATH:-$(command -v flatc || true)}
if [ -n "$flatc" ]; then
    out="$root/samples/game/target/flatc"
    "$flatc" --java --ts -o "$out" expected/game.fbs
    echo "OK: flatc compiled expected/game.fbs for Java and TypeScript (into target/flatc)"
elif [ "${CI:-}" = "true" ]; then
    echo "FLATC_PATH must be set in CI"
    exit 1
else
    echo "Skipped flatc: none on the PATH and FLATC_PATH is not set"
fi
exit $status
