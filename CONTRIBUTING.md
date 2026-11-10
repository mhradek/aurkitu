# Contributing to Aurkitu

Thanks for helping. Bug reports, fixes and new FlatBuffers features are all welcome.

## Reporting

- **Bugs and feature requests:** open an issue using one of the templates. For a bug, the annotated class, the
  plugin configuration and the schema you got (or the error) usually make the cause clear.
- **Security problems:** do not open a public issue. See [SECURITY.md](SECURITY.md).

## Building

Requires JDK 17 or newer and Maven 3.6.3 or newer; the Gradle plugin also needs Gradle 8.14 or newer.

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
core you just installed. Its `build` runs the unit tests with their coverage gate and the functional tests,
which build the Maven end-to-end projects with Gradle too:

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
  both plugins with the same name and default: a `@Parameter` (with its `aurkitu.*` property) in `AurkituMojo`,
  and a property of `AurkituExtension` and `GenerateFlatBuffersSchema` with its default and `aurkitu.*` Gradle
  property in `AurkituPlugin`. `PluginParityAndPublishingTest` fails if the two plugins' options or defaults
  differ.
- **The annotations are a public API.** japicmp fails the build on incompatible changes; a new annotation
  element needs a default and a minor version.
- **SpotBugs findings** are fixed, not excluded. If one is a false positive, add it to `spotbugs-exclude.xml`
  with the reason.
- **Javadoc** for anything users see: annotation elements and plugin parameters. Say what it writes in the schema
  and where it is valid.
- **Documentation**: a user-visible change also updates the README (the option table, and the Maven or Gradle
  section when it concerns one plugin) and `CHANGELOG.md`. Check snippets in a real build.

## Pull requests

Keep a pull request to one change. CI runs the Maven build on Java 17, 21 and 25, the Gradle plugin on Gradle
8.14 and 9.9, CodeQL, Codacy and the coverage reports; all of it must pass. Note user-visible changes in `CHANGELOG.md` under "Unreleased".

By contributing, you agree that your contributions are licensed under the project's [MIT license](LICENSE.md).
