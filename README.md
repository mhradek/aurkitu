# Aurkitu 1.0.0

[![Latest release](https://img.shields.io/github/release/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-maven-plugin.svg?label=plugin)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-maven-plugin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-annotations.svg?label=annotations)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-annotations)
[![Build Status](https://github.com/mhradek/aurkitu/actions/workflows/main.yml/badge.svg)](https://github.com/mhradek/aurkitu/actions)
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
| `mapCommonTypes` | No | `true` | Map `Date`, `URL`, `BigDecimal`, `UUID`, etc. to `string` |
| `snakeCaseFields` | No | `true` | Convert Java field names to snake_case in the schema |
| `generateVersion` | No | `false` | Emit a content hash of the schema (excluding validation comments) as `// @version: <hash>` |
| `failOnValidationIssues` | No | `false` | Fail the build when validation issues are found |
| `fileExtension` | No | — | Emit `file_extension "..."` |

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

`byte[]` fields are mapped to `[ubyte]` (the common FlatBuffers binary vector convention):

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

```java
@FlatBufferEnum(enumType = FieldType.BYTE)
public enum SampleEnumByte {
    @FlatBufferEnumTypeField
    byte id;
    String description;
    // ...
}
```

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
- **Structs**: Non-scalar fields are flagged. Set `failOnValidationIssues` to skip them instead of emitting an invalid struct.
- **Includes**: `include` lines are emitted only. Cross-schema types are not resolved.
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

When `validate` is enabled, issues are appended as comments (unsupported fields, non-scalar struct fields, missing root type). Set `failOnValidationIssues` to fail the build instead. Optional `flatcPath` runs `flatc --java` on the generated schema.

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
| **schema caching** | Skip if file exists | Content-aware hash (types + config) | [Configuration](#configuration-options) |
| **failOnValidationIssues** | Not present | Optional fail-the-build | [Configuration](#configuration-options) |
| **specifiedDependencies** | Artifact-level | `specifiedPackages` (package-level) | [Configuration](#configuration-options) |
| **consolidatedSchemas** | Supported | Removed (single schema) | — |
| **namespaceOverrideMap** | Supported | Per-type `namespace` line | [Configuration](#configuration-options) |
| **flatc validation** | Manual / script | Optional `flatcPath` + CI binary | [Optional flatc validation](#optional-flatc-validation) |
| **Tests** | Mixed | Pure JUnit 5 + flatc test | — |
| **CI** | Basic | JDK 17 + official flatc download | [Optional flatc validation](#optional-flatc-validation) |

## Roadmap

1.0.0 is published to Maven Central. Remaining work:

| Priority | Item | Why it matters | Status |
|----------|------|----------------|--------|
| **Medium** | Upload JaCoCo to codecov or coveralls | Coverage badge accuracy | Reports generated locally only |
| **Medium** | Published Javadoc site | Consumer docs beyond the README | Jars are attached on Central |
| **Low** | True multi-namespace schema splitting | Advanced use case | Per-type `namespace` line only |
| **Low** | Schema include resolution | Cross-schema types | Includes are emitted only |
| **Low** | Configurable binary vector (`[byte]` vs `[ubyte]`) | Flexibility | Fixed to `[ubyte]` |
| **Low** | `rpc_service` and `///` doc comments | Official schema completeness | Not generated |
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
| `enum` | Yes | Yes | Match; explicit values emitted |
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
| Multiple namespaces | Per-type `namespace` line | Supported | Basic support |
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
