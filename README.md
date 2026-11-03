# Aurkitu 1.0.0

[![Latest release](https://img.shields.io/github/release/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/releases/latest)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/42b23d745afc4ef1b1102807752e8a38)](https://www.codacy.com/gh/mhradek/aurkitu/dashboard?utm_source=github.com&utm_medium=referral&utm_content=mhradek/aurkitu&utm_campaign=Badge_Grade)
[![Build Status](https://github.com/mhradek/aurkitu/actions/workflows/main.yml/badge.svg)](https://github.com/mhradek/aurkitu/actions)
[![codecov](https://codecov.io/gh/mhradek/aurkitu/branch/master/graph/badge.svg)](https://codecov.io/gh/mhradek/aurkitu)
[![Coverage Status](https://coveralls.io/repos/github/mhradek/aurkitu/badge.svg?branch=master)](https://coveralls.io/github/mhradek/aurkitu?branch=master)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-maven-plugin.svg)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-maven-plugin)

**Aurkitu** (Basque for “to find”) generates FlatBuffers IDL (`.fbs`) schemas from annotated Java models. This keeps your server-side models as the single source of truth — especially useful when models carry storage annotations (e.g. DynamoDB) that must remain authoritative.

This is the **modernised 1.0.0** release:
- Targets **Java 17**
- Uses **ClassGraph** for reliable classpath scanning (replaces the old custom Reflections fork and fragile classloader logic)
- Simplified architecture with clearer separation of concerns
- Updated dependencies and Maven plugin toolchain
- Oriented toward current FlatBuffers (25.x series)
- **New coordinates**: `io.github.mhradek` (migrated from `com.michaelhradek`)

*Note: 1.0.0 is a clean modern foundation. Full field-level processing, advanced type support, and complete `flatc` validation are being expanded on top of the ClassGraph scanner. The original 0.0.x behaviour is the reference for historical edge cases.*

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

Additional options from the 0.0.x series (namespace overrides, schema includes, consolidated vs per-dependency schemas, schema caching, ignore static members, file identifier, etc.) will be re-introduced as the generator matures.

---

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

## Peculiarities & limitations (historical + current)

- **Unsigned types**: FlatBuffers supports unsigned primitives; Java does not. 0.0.x mapped wrappers to signed primitives. Future 1.x work may improve this.
- **Maps**: The 0.0.x series generated intermediate `MapValueSet_<type>_<field>` tables (a workaround). Clients and servers needed custom translation code. This pattern will be revisited.
- **Lists / arrays**: Supported for simple (non-parameterised) element types.
- **Inner classes**: `$` in type names can cause issues; prefer `useFullName` where applicable.
- **Scanning performance**: Always prefer `specifiedPackages` (or the older `specifiedDependencies`) to avoid full classpath scans.
- **Java baseline**: 1.0.0 requires JDK 17+. The original series targeted Java 8.

---

## Sample output (illustrative, from 0.0.x tests)

```idl
// Aurkitu automatically generated IDL FlatBuffer Schema
namespace generated.flatbuffers;

enum SampleEnumByte : byte { EnvironmentAlpha = 1, EnvironmentBeta = 2, EnvironmentGamma = 3 }

table SampleClassTable {
  id:long;
  name:string;
  // ...
}

root_type SampleClassTable;
```

---

## Validation

When `validate` is enabled, comments describing issues (missing type definitions, invalid paths containing `$`, empty enums, bad namespaces, etc.) can be appended to the generated schema. This helps catch problems before running `flatc`.

Example (from original series):
```idl
// Schema failed validation (i.e. flatc will likely fail):
// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples
// Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'...
```

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
| **Maps** | `MapValueSet_<type>_<field>` | Same pattern (snake_case aware) | [Peculiarities](#peculiarities--limitations-historical--current) |
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
| **namespaceOverrideMap** | Supported | Per-type `namespace` comment | [Configuration](#configuration-options) |
| **flatc validation** | Manual / script | Optional `flatcPath` + CI binary | [Optional flatc validation](#optional-flatc-validation) |
| **Tests** | Mixed | Pure JUnit 5 + flatc test | — |
| **CI** | Basic | JDK 17 + official flatc download | [Optional flatc validation](#optional-flatc-validation) |

## Roadmap (post-1.0.0)

### Near term
- [ ] Complete Maven Central publishing (add GPG + Central Portal secrets and enable `mvn deploy` in publish workflow)
- [ ] Confirm generated `plugin.xml` descriptor in a release build
- [ ] Expand Mojo integration tests (full plugin invocation with `maven-plugin-testing`)
- [ ] Upload JaCoCo coverage to codecov/coveralls

### Future
- [ ] True multi-namespace schema splitting (separate files or namespace blocks)
- [ ] Optional attributes beyond `id` (e.g. `key`, `hash`)
- [ ] Schema include resolution and cross-schema type references
- [ ] Configurable binary vector type (`[byte]` vs `[ubyte]`)

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

