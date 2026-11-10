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
maven_args=()
gradle_args=()
if [ "${1:-}" = "--this-checkout" ]; then
    version=$(cd "$root" && mvn -B -q help:evaluate -Dexpression=project.version -DforceStdout)
    echo "Building the sample against this checkout ($version)"
    maven_args=("-DaurkituVersion=$version")
    gradle_args=("-PaurkituVersion=$version" --include-build "$root/aurkitu-gradle-plugin")
fi

cd "$root/samples/game"
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
