# Contributing to Aurkitu

Thanks for helping. Bug reports, fixes and new FlatBuffers features are all welcome.

## Reporting

- **Bugs and feature requests:** open an issue using one of the templates. For a bug, the annotated class, the
  plugin configuration and the schema you got (or the error) usually make the cause clear.
- **Security problems:** do not open a public issue. See [SECURITY.md](SECURITY.md).

## Building

Requires JDK 17 or newer and Maven 3.6.3 or newer.

```bash
mvn clean verify
```

This runs the unit tests, the end-to-end projects in `aurkitu-maven-plugin/src/it`, the coverage gate,
SpotBugs and the API compatibility check. To also compile every generated schema with `flatc`, as CI does,
point `FLATC_PATH` at a `flatc` binary (the version CI uses is `FLATC_VERSION` in
`.github/workflows/main.yml`):

```bash
FLATC_PATH=/path/to/flatc mvn clean verify
```

If you change the core (`aurkitu-core`) or the Gradle plugin, also build the Gradle plugin, which uses the
core you just installed:

```bash
mvn -B install -DskipTests -Dinvoker.skip
```

```bash
cd aurkitu-gradle-plugin && gradle build
```

The README's "Building Aurkitu" section describes each check.

## Making a change

- **Tests come with the change.** Compare whole schemas rather than substrings, so an unexpected field or
  attribute fails the test. Every class must keep 80% line and branch coverage.
- **Output changes are deliberate.** If the generated schema changes, update the expected files in
  `src/it/*/expected` in the same commit and say why in the pull request.
- **Validation matches `flatc`.** A new rule reports exactly what `flatc` rejects; `FlatcAgreementTest` checks
  this when `flatc` is available.
- **New generator options** go into `OptionMatrixTest`, which checks every combination of options, and into
  both plugins: a `@Parameter` in `AurkituMojo` and a property of the Gradle extension and task, with the same
  name and default.
- **The annotations are a public API.** japicmp fails the build on incompatible changes; a new annotation
  element needs a default and a minor version.
- **SpotBugs findings** are fixed, not excluded. If one is a false positive, add it to `spotbugs-exclude.xml`
  with the reason.
- **Javadoc** for anything users see: annotation elements and plugin parameters. Say what it writes in the schema
  and where it is valid.

## Pull requests

Keep a pull request to one change. CI runs the build on Java 17, 21 and 25, CodeQL and the coverage reports;
all of it must pass. Note user-visible changes in `CHANGELOG.md` under "Unreleased".

By contributing, you agree that your contributions are licensed under the project's [MIT license](LICENSE.md).
