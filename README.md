# Aurkitu 1.0.0

[![Latest release](https://img.shields.io/github/release/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-maven-plugin.svg?label=plugin)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-annotations.svg?label=annotations)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-annotations)
[![Build Status](https://github.com/mhradek/aurkitu/actions/workflows/main.yml/badge.svg)](https://github.com/mhradek/aurkitu/actions)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/42b23d745afc4ef1b1102807752e8a38)](https://app.codacy.com/gh/mhradek/aurkitu/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![codecov](https://codecov.io/gh/mhradek/aurkitu/branch/master/graph/badge.svg)](https://codecov.io/gh/mhradek/aurkitu)
[![Coverage Status](https://coveralls.io/repos/github/mhradek/aurkitu/badge.svg?branch=master)](https://coveralls.io/github/mhradek/aurkitu?branch=master)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/stargazers)

**Aurkitu** (Basque for “to find”) generates FlatBuffers IDL (`.fbs`) schemas from annotated Java models. This keeps your server-side models as the single source of truth — especially useful when models carry storage annotations (e.g. DynamoDB) that must remain authoritative.

This is the **modernised 1.0.0** release:

- Targets **Java 17**
- Uses **ClassGraph** for reliable classpath scanning (replaces the old custom Reflections fork and fragile classloader logic)
- Simplified architecture with clearer separation of concerns
- Updated dependencies and Maven plugin toolchain
- Oriented toward current FlatBuffers (25.x series)
- **New coordinates**: `io.github.mhradek` (migrated from `com.michaelhradek`)

**1.0.0 is published** to Maven Central under `io.github.mhradek`. It generates schemas for tables, structs, enums, unions, maps, inheritance, and field options, and can optionally validate them with `flatc`. See [Changes from 0.0.x](#changes-from-00x-to-100) if you are upgrading.

---

## Coverage

JaCoCo is the **single source of truth**. `mvn verify` fails if **any class** drops below **80% line coverage or 80% branch coverage** (see `jacoco.line.coverage.minimum` and `jacoco.branch.coverage.minimum` in the parent POM). The check is per class, so a well-tested module cannot hide an untested class.

External badges (Codecov, Coveralls, Codacy) are mirrors. They require these repository secrets:

| Secret | Tool | Required |
|--------|------|----------|
| `CODECOV_TOKEN` | Codecov | Yes (upload fails the job) |
| `COVERALLS_REPO_TOKEN` | Coveralls | Yes (upload fails the job) |
| `CODACY_PROJECT_TOKEN` | Codacy Coverage Reporter | Optional (step is skipped if absent) |

Codacy’s quality-grade badge is independent of coverage; coverage appears only after a successful Codacy Coverage Reporter upload.

---

## Quick Start

### Annotations

```xml
<dependency>
    <groupId>io.github.mhradek</groupId>
    <artifactId>aurkitu-annotations</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Maven Plugin

```xml
<plugin>
    <groupId>io.github.mhradek</groupId>
    <artifactId>aurkitu-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <schemaName>user</schemaName>
        <schemaNamespace>com.example.flatbuffers</schemaNamespace>
        <!-- Optional but strongly recommended for speed -->
        <specifiedPackages>
            <specifiedPackage>com.example.models</specifiedPackage>
        </specifiedPackages>
        <validate>true</validate>
    </configuration>
    <executions>
        <execution>
            <phase>process-classes</phase>
            <goals>
                <goal>build-schema</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Annotate models

```java
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(rootType = true)
public class User {
    private long id;
    private String name;
    // ...
}
```

Run `mvn process-classes` (or a later phase) to generate `target/aurkitu/schemas/user.fbs`.

---

## Configuration options

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| `schemaName` | Yes | — | Output filename stem (produces `schemaName.fbs`) |
| `schemaNamespace` | No | `generated.flatbuffers` | FlatBuffers namespace |
| `outputDir` | No | `${project.build.directory}/aurkitu/schemas` | Destination directory |
| `specifiedPackages` | No | all packages | Limit ClassGraph scanning for performance |
| `validate` | No | `true` | Enable basic schema validation reporting |
| `ignoreStaticMembers` | No | `true` | Skip static fields when generating tables |
| `fileIdentifier` | No | — | Optional 4-character file identifier |
| `includes` | No | — | List of schema files to `include` |
| `mapCommonTypes` | No | `true` | Map `Date`, `UUID`, `URL`, `URI`, `BigDecimal`, `BigInteger` and `java.time` types (`Instant`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `ZonedDateTime`, `Duration`) to `string`. `String` is always mapped. |
| `snakeCaseFields` | No | `true` | Convert Java field names to snake_case in the schema |
| `generateVersion` | No | `false` | Emit a content hash of the schema body as `// @version: <hash>`. The header and validation comments are excluded, so the hash changes only when the schema does. |
| `failOnValidationIssues` | No | `false` | Fail the build when validation issues are found |
| `fileExtension` | No | — | Emit `file_extension "..."` |
| `flatcPath` | No | — | Path to a `flatc` binary. When set (and `validate` is on), the schema is checked with `flatc --binary --schema` and the build fails if `flatc` rejects it |
| `cacheEnabled` | No | `false` | Only rewrite the schema file when its content changes, keeping its timestamp for downstream steps. The schema is still generated and validated every time |

The plugin scans the project's compile classpath (its `target/classes` plus compile-scope dependencies), so annotated models can live in the project itself or in a dependency.

---

### Unsigned types

Java has no unsigned primitives. Use `fieldType` to emit unsigned FlatBuffers types:

```java
@FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
private byte flags;    // → flags:ubyte;

@FlatBufferFieldOptions(fieldType = FieldType.USHORT)
private short port;    // → port:ushort;

@FlatBufferFieldOptions(fieldType = FieldType.UINT)
private int count;     // → count:uint;

@FlatBufferFieldOptions(fieldType = FieldType.ULONG)
private long id;       // → id:ulong;
```

### Binary data and field ids

`byte[]` fields are mapped to `[ubyte]` (the common FlatBuffers binary vector convention). Use `@FlatBufferFieldOptions(fieldType = FieldType.BYTE)` for `[byte]`:

```java
@FlatBufferTable
public class Blob {
    private byte[] payload;   // → payload:[ubyte];
}
```

Optional explicit field ids can be set with `fieldId` (use `-1` to omit):

```java
@FlatBufferFieldOptions(fieldId = 0, required = true)
private String id;

@FlatBufferFieldOptions(fieldId = 1)
private int version;
```

emits:

```idl
id:string (required) (id: 0);
version:int (id: 1);
```

## Usage examples

### Root type

```java
@FlatBufferTable(rootType = true)
public class SampleClassTable {
    // fields...
}
```

### Struct vs table

```java
@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class SampleClassStruct {
    private float x;
    private float y;
    private float z;
}
```

### Enums

Without a value field, constants are emitted in declaration order with implicit values. `enumType` sets the underlying type (`BYTE` by default; unsigned types are supported):

```java
@FlatBufferEnum(enumType = EnumType.UINT)
public enum Color { RED, GREEN, BLUE }   // enum Color : uint { RED, GREEN, BLUE, }
```

To emit explicit values, mark an integral field with `@FlatBufferEnumTypeField`. Values are emitted in ascending order; duplicates and out-of-range values are reported:

```java
@FlatBufferEnum(enumType = EnumType.SHORT)
public enum Priority {
    HIGH((short) 30), LOW((short) 10);

    @FlatBufferEnumTypeField
    private final short weight;

    Priority(short weight) { this.weight = weight; }
}
// enum Priority : short { LOW = 10, HIGH = 30, }
```

A table field of enum type defaults to `0`. If the enum has no value `0`, set a default that is one of its constants, e.g. `@FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "LOW")`; otherwise `flatc` rejects the schema, so this is reported as a validation issue.

Unions use `@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)`; each constant names a table.

### Namespaces

`@FlatBufferTable(namespace = "...")` and `@FlatBufferEnum(namespace = "...")` place one type in another namespace. The default namespace is restored straight after it, and references across namespaces (including `root_type`) are fully qualified.

### Ignore fields

```java
@FlatBufferIgnore
protected String ignore;
```

### Field options (type overrides, defaults, full name, etc.)

```java
@FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SHORT_SWORD")
protected SampleClassTableInnerEnumInt innerEnum;
```

---

## Limitations

- **Unsigned types**: Java has no unsigned primitives. Use `fieldType` (`UBYTE`, `USHORT`, `UINT`, `ULONG`) to emit them.
- **Maps**: FlatBuffers has no map type. Aurkitu emits `MapValueSet_<Owner>_<field>` tables. Clients still need a small translation layer.
- **Partial `fieldId`**: If any field in a table has `fieldId`, every field should. Partial ids can make `flatc` reject the schema.
- **Structs**: Fields that are not scalars, enums, structs or fixed-length arrays of those (e.g. strings, vectors, tables, unions) are flagged. Set `failOnValidationIssues` to skip them instead of emitting an invalid struct. Structs are declared before the structs and tables that use them. A struct cannot be the root type.
- **Type names**: Types are declared by their simple Java name (nested `Outer.Inner` becomes `Inner`). Two types with the same simple name in one namespace are reported as a validation issue.
- **Nested types**: Classes and enums nested directly in an annotated table or struct are declared automatically, in the outer type's namespace, even without their own annotation (as in 0.0.x). Mark a nested type `@FlatBufferIgnore` to leave it out. Deeper nesting is not followed.
- **Referenced types**: A field (or union member) must reference a type the schema declares. Without `includes` this is checked and reported; with `includes` it is left to `flatc`, since included schemas are not resolved.
- **Unsupported types**: `char`, nested vectors (`int[][]`, `List<List<T>>`) and JDK types outside the common-type list are skipped and reported.
- **Includes**: `include` lines are emitted only. Cross-schema types are not resolved (see *Referenced types*).
- **Scanning**: Prefer `specifiedPackages` so ClassGraph does not scan the whole classpath.
- **Not generated**: `rpc_service` and `///` doc comments.

---

## Sample output

```idl
// Aurkitu 1.0.0 automatically generated FlatBuffer schema
// @version: 447551ba
namespace com.example;

file_identifier "AB12";

enum SampleEnum : byte {
  ALPHA = 0,
  BETA = 1,
  GAMMA = 2
}

table SampleRootTable {
  id:long;
  name:string;
  level:int = 42;
  tags:[string];
  status:SampleEnum;
}

root_type SampleRootTable;
```

---

## Validation

When `validate` is enabled, issues are appended as comments (unsupported fields, references to undeclared types, non-scalar struct fields, partial field ids, misplaced field attributes, enum problems such as missing, duplicate or out-of-range values, invalid union members, invalid namespaces, duplicate type names, a missing or non-table root type). Set `failOnValidationIssues` to fail the build instead. Optional `flatcPath` checks the generated schema with `flatc --binary --schema`.

---

## Compiling schemas with flatc

Download or build `flatc` from the [FlatBuffers](https://github.com/google/flatbuffers) project (current 25.x series recommended).

```bash
# Example
flatc --java --gen-mutable -o target/aurkitu/output/java target/aurkitu/schemas/*.fbs
flatc --cpp -o target/aurkitu/output/cpp target/aurkitu/schemas/*.fbs
```

This can be automated further via the Maven exec plugin or a dedicated FlatBuffers Maven plugin.

---

## Building Aurkitu itself

Requires JDK 17+.

```bash
mvn clean verify
```

`verify` runs the unit tests and then the end-to-end tests in `aurkitu-maven-plugin/src/it`. Each directory there is a real consumer project, built with [maven-invoker-plugin](https://maven.apache.org/plugins/maven-invoker-plugin/) against the freshly built plugin:

| Project | Covers |
|---------|--------|
| `kitchen-sink` | Every annotation and field attribute, enums/unions/structs, inheritance, nested classes, maps, common types, includes, per-type namespace |
| `settings-variants` | `snakeCaseFields=false`, `mapCommonTypes=false`, `ignoreStaticMembers=false`, `generateVersion`, validation comments |
| `fail-on-validation` | `failOnValidationIssues=true` fails the build |
| `flatc-path` | The `flatcPath` option (runs only when `FLATC_PATH` is set) |
| `dependency-models` | Annotated models in a dependency jar (two-module build), including a nested enum |

Each `verify.groovy` compares the generated schema with `expected/*.fbs` and, when `FLATC_PATH` points to a `flatc` binary, compiles it with `flatc --cpp --java`. In CI (`CI=true`) a missing `FLATC_PATH` fails the build rather than skipping the check. If you change the output on purpose, update the expected file in the same commit. Use `-Dinvoker.skip` to skip these tests.

---

## Changes from 0.0.x to 1.0.0

| Area | 0.0.x | 1.0.0 | Documentation |
|------|-------|-------|---------------|
| **Coordinates** | `com.michaelhradek` | `io.github.mhradek` | [Migration](#migration-from-00x-to-100) |
| **Java baseline** | 8 | 17 | [Building](#building-aurkitu-itself) |
| **Scanning** | Custom Reflections fork | ClassGraph | [Quick Start](#quick-start) |
| **Field names** | camelCase (as-is) | snake_case by default | [Migration](#migration-from-00x-to-100), [Configuration](#configuration-options) |
| **Maps** | `MapValueSet_<type>_<field>` | Same pattern (snake_case aware) | [Limitations](#limitations) |
| **required / deprecated** | Planned | Implemented | [Configuration](#configuration-options) |
| **Unions** | Limited | Implemented | — |
| **Inheritance** | Not supported | Superclass fields included | — |
| **Common types** | Not implemented | Date, URL, BigDecimal, UUID, … → string | [Configuration](#configuration-options) |
| **byte[]** | Not special-cased | Mapped to `[ubyte]` | — |
| **fieldId** | Not present | Optional `id: N` | [Configuration](#configuration-options) |
| **generateVersion** | Supported | Supported (`// @version: <hash>`) | [Configuration](#configuration-options) |
| **schema caching** | Skip if file exists | Rewrite only when the generated content changes | [Configuration](#configuration-options) |
| **Nested types** | Declared automatically | Declared automatically (restored in 1.0.1; 1.0.0 dropped them) | [Limitations](#limitations) |
| **`@FlatBufferOptions`** | No effect | Deprecated (no effect) | — |
| **failOnValidationIssues** | Not present | Optional fail-the-build | [Configuration](#configuration-options) |
| **specifiedDependencies** | Artifact-level | `specifiedPackages` (package-level) | [Configuration](#configuration-options) |
| **consolidatedSchemas** | Supported | Removed (single schema) | — |
| **namespaceOverrideMap** | Supported | Per-type `namespace` line | [Configuration](#configuration-options) |
| **flatc validation** | Manual / script | Optional `flatcPath` + CI binary | [Optional flatc validation](#optional-flatc-validation) |
| **Tests** | Mixed | Pure JUnit 5 + flatc test | — |
| **CI** | Basic | JDK 17 + official flatc download | [Optional flatc validation](#optional-flatc-validation) |

## Roadmap

1.0.0 is published to Maven Central. Remaining work (1.0.1 fixes are listed in the pull request and release notes):

| Priority | Item | Why it matters | Status |
|----------|------|----------------|--------|
| **Done** | Coverage gate (JaCoCo check + hardened uploads) | Reliable coverage numbers | 80% line and branch per class; uploads fail the job |
| **Done** | End-to-end tests | Real consumer builds, checked with `flatc` | `src/it` (invoker) |
| **Medium** | Published Javadoc site | Consumer docs beyond the README | Jars are attached on Central |
| **Low** | True multi-namespace schema splitting | Advanced use case | Per-type `namespace` line only |
| **Low** | Schema include resolution | Cross-schema types | Includes are emitted only |
| **Done** | Configurable binary vector (`[byte]` vs `[ubyte]`) | Flexibility | `[ubyte]` by default; `fieldType = FieldType.BYTE` gives `[byte]` |
| **Low** | `rpc_service` and `///` doc comments | Official schema completeness | Not generated |
| **Low** | Remove deprecated `@FlatBufferOptions` | API hygiene | Deprecated; remove in 2.0 |
| **Done** | Maven Central publishing | 1.0.0 is live | Done |
| **Done** | `plugin.xml` descriptor | Installable plugin | Done |
| **Done** | Mojo integration tests | Parameter binding | Done |
| **Done** | Field attributes (`key`, `hash`, `nested_flatbuffer`, `flexbuffer`, `force_align`, `id`) | Schema completeness | Done |

### Done in 1.0.0 (summary)

Java 17, ClassGraph scanner, field processing (primitives, lists, nested maps, enums, unions, structs, inheritance), field options (type override, required, deprecated, defaults, comments, useFullName, fieldId), common type mapping, snake_case (default on), generateVersion, content-aware cache, failOnValidationIssues, optional flatc validation + CI binary, pure JUnit 5 + flatc test, Mojo config tests, JaCoCo, migration guide, Codacy issues addressed.

## FlatBuffers schema compatibility

Comparison of Aurkitu 1.0.0 output against the [official FlatBuffers schema documentation](https://flatbuffers.dev/schema/).

| Feature | Aurkitu 1.0.0 | Official schema | Notes |
|---------|---------------|-----------------|-------|
| `namespace` | Yes | Yes | Match; per-type override emits a new `namespace` line |
| `file_identifier` | Yes | Yes | Match |
| `file_extension` | Yes | Yes | Match |
| `include` | Yes | Yes | Match (emitted only; not resolved) |
| `root_type` | Yes | Yes | Match |
| `table` / `struct` | Yes | Yes | Match |
| Struct constraints | Warned | Scalars/structs only | Non-scalar fields in structs produce a validation issue |
| `enum` | Yes | Yes | Match; explicit values via `@FlatBufferEnumTypeField` |
| `union` | Yes | Yes | Match; `NONE` added by `flatc` |
| Field defaults | Yes | Scalars only | Match for scalars |
| `(required)` / `(deprecated)` | Yes | Yes | Match |
| `(id: N)` | Yes | Yes | Partial use can invalidate schema; use on all fields or none |
| `(key)` | Yes | Yes | Match |
| `(hash: "...")` | Yes | Yes | Match |
| `(nested_flatbuffer)` | Yes | Yes | Match |
| `(flexbuffer)` | Yes | Yes | Match |
| `(force_align: N)` | Yes | Yes | Match |
| Fixed-size arrays `[type:N]` | Yes | Yes | Via `fixedLength` |
| `byte[]` → `[ubyte]` | Yes | Valid | Match |
| snake_case fields | Default on | Recommended | Match |
| `///` doc comments | No | Supported | Minor gap |
| `rpc_service` | No | Supported | Not implemented |
| Unsigned types | Via `fieldType` | Full set | Supported |
| Multiple namespaces | Per-type `namespace` | Supported | Scoped to the type; cross-namespace references qualified |
| Map workaround | `MapValueSet_*` | No native map | Standard workaround |

### Remaining discrepancies

- Partial `fieldId` use (some fields only) can produce schemas `flatc` rejects.
- Structs with non-scalar fields are flagged but still emitted unless `failOnValidationIssues` is true.
- `rpc_service` and `///` documentation comments are not generated.
- Unsigned types are supported via `fieldType` (`UBYTE`, `USHORT`, `UINT`, `ULONG`).

## License

MIT

---

## Migration from 0.0.x to 1.0.0

### Coordinates

| | 0.0.x | 1.0.0 |
|---|-------|-------|
| groupId | `com.michaelhradek` | `io.github.mhradek` |
| Java packages | `com.michaelhradek.aurkitu.*` | `io.github.mhradek.aurkitu.*` |

### Key changes

- **Java baseline**: 8 → 17
- **Scanning**: Custom Reflections fork → ClassGraph
- **Architecture**: Monolithic `Processor` → simplified `SchemaGenerator` + Mojo
- **Tests**: Mixed → pure JUnit 5
- **Maps**: Still use generated `MapValueSet_*` tables (workaround unchanged)
- **Field names (breaking)**: 0.0.x emitted Java field names as-is (camelCase). 1.0.0 converts them to snake_case by default (`nestedMap` → `nested_map`) to follow modern FlatBuffers style. Set `<snakeCaseFields>false</snakeCaseFields>` to preserve the old behaviour.
- **New options**: `ignoreStaticMembers`, `fileIdentifier`, `includes`, `mapCommonTypes`, `snakeCaseFields` (default `true`)

### Migration steps

1. Update dependency/plugin coordinates to `io.github.mhradek`.
2. Change imports from `com.michaelhradek.aurkitu.*` to `io.github.mhradek.aurkitu.*`.
3. Rebuild and re-generate schemas.
4. Review any custom translation code that relies on the old `MapValueSet` naming (names are similar but not guaranteed identical).
5. Prefer `specifiedPackages` for faster scans.

---

## Optional flatc validation

### CI setup

GitHub Actions (`.github/workflows/main.yml`) automatically downloads the official `flatc` 25.12.19 Linux binary and makes it available for the build:

- The binary is placed on `PATH` and exported as `FLATC_PATH`.
- `mvn verify` runs `FlatcValidationTest`, which generates a representative schema and invokes `flatc --java` against it.
- The test is skipped automatically when `flatc` is not present (so local builds without it still pass).

To run the validation test locally, install `flatc` or set `FLATC_PATH` to the binary, then run:

```bash
mvn -B clean verify
```

### Via the plugin

Set `flatcPath` to the location of the `flatc` binary. When `validate` is true the generated schema is checked:

```xml
<configuration>
    <schemaName>user</schemaName>
    <flatcPath>/usr/local/bin/flatc</flatcPath>
    <validate>true</validate>
</configuration>
```

### Via the Maven exec plugin (alternative)

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>validate-schema</id>
            <phase>verify</phase>
            <goals><goal>exec</goal></goals>
            <configuration>
                <executable>flatc</executable>
                <arguments>
                    <argument>--schema</argument>
                    <argument>${project.build.directory}/aurkitu/schemas/user.fbs</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```
