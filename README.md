# Aurkitu 1.0.1

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

The 1.0 line:

- Targets **Java 17**
- Uses **ClassGraph** to scan the project's compile classpath (replaces the old custom Reflections fork and classloader logic)
- Builds a typed schema model, validates it, and lets each element write its own schema syntax
- Is tested end to end against real consumer builds, and the generated schemas are compiled with `flatc` 25.x in CI
- **Coordinates**: `io.github.mhradek` (migrated from `com.michaelhradek`)

It generates schemas for tables, structs, enums, unions, maps, inheritance and field options, validates them, and can optionally check them with `flatc`.

> **Upgrading from 1.0.0?** 1.0.0 scanned the plugin's own classpath instead of your project's, so it generated an empty schema in real builds. Use 1.0.1. See [What's new in 1.0.1](#whats-new-in-101).
>
> **Upgrading from 0.0.x?** See [Migration from 0.0.x](#migration-from-00x).

---

## Quick start

### Annotations

```xml
<dependency>
    <groupId>io.github.mhradek</groupId>
    <artifactId>aurkitu-annotations</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Maven plugin

```xml
<plugin>
    <groupId>io.github.mhradek</groupId>
    <artifactId>aurkitu-maven-plugin</artifactId>
    <version>1.0.1</version>
    <configuration>
        <schemaName>user</schemaName>
        <schemaNamespace>com.example.flatbuffers</schemaNamespace>
        <!-- Optional but recommended: limits scanning to your model packages -->
        <specifiedPackages>
            <specifiedPackage>com.example.models</specifiedPackage>
        </specifiedPackages>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>build-schema</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The `build-schema` goal runs in the `process-classes` phase by default, after your classes are compiled.

### Annotate models

```java
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

@FlatBufferTable(rootType = true)
public class User {
    private long id;
    private String name;
}
```

Run `mvn process-classes` (or any later phase, such as `mvn package`) to generate `target/aurkitu/schemas/user.fbs`.

---

## Configuration options

| Parameter | Property | Default | Description |
|-----------|----------|---------|-------------|
| `schemaName` | `aurkitu.schemaName` | — (required) | Output file name stem (`<schemaName>.fbs`) |
| `schemaNamespace` | `aurkitu.schemaNamespace` | `generated.flatbuffers` | Default FlatBuffers namespace |
| `outputDirectory` | `aurkitu.outputDir` | `${project.build.directory}/aurkitu/schemas` | Destination directory |
| `specifiedPackages` | `aurkitu.specifiedPackages` | all packages | Packages to scan (and their subpackages) |
| `validate` | `aurkitu.validate` | `true` | Report [validation](#validation) issues as comments at the end of the schema |
| `failOnValidationIssues` | `aurkitu.failOnValidationIssues` | `false` | Fail the build when there are validation issues |
| `ignoreStaticMembers` | `aurkitu.ignoreStaticMembers` | `true` | Skip static fields |
| `snakeCaseFields` | `aurkitu.snakeCaseFields` | `true` | Write field names in snake_case (`displayName` → `display_name`) |
| `mapCommonTypes` | `aurkitu.mapCommonTypes` | `true` | Map `Date`, `UUID`, `URL`, `URI`, `BigDecimal`, `BigInteger` and `java.time` types (`Instant`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `ZonedDateTime`, `Duration`) to `string`. `String` is always mapped. |
| `fileIdentifier` | `aurkitu.fileIdentifier` | — | Emit `file_identifier "..."` (must be 4 characters) |
| `fileExtension` | `aurkitu.fileExtension` | — | Emit `file_extension "..."` |
| `includes` | `aurkitu.includes` | — | Schema files to `include` |
| `generateVersion` | `aurkitu.generateVersion` | `false` | Emit `// @version: <hash>`, a hash of the schema body. The header and validation comments are excluded, so it changes only when the schema does. |
| `flatcPath` | `aurkitu.flatcPath` | — | Path to a `flatc` binary. When set (and `validate` is on), the schema is checked with `flatc --binary --schema` and the build fails if `flatc` rejects it. |
| `cacheEnabled` | `aurkitu.cacheEnabled` | `false` | Only rewrite the schema file when its content changes, keeping its timestamp for downstream steps. The schema is still generated and validated every time. |
| `optionalScalars` | `aurkitu.optionalScalars` | `false` | Write boxed Java scalars (`Integer`, `Boolean`, …) in tables as [optional scalars](#optional-scalars) (`= null`). Since 1.1.0. |

The plugin scans the project's compile classpath (its compiled classes plus compile-scope dependencies), so annotated models can live in the project itself or in a dependency jar.

---

## Usage

### Tables, structs and the root type

```java
@FlatBufferTable(rootType = true)
public class Account { ... }

@FlatBufferTable(value = FlatBufferTable.TableStructureType.STRUCT)
public class Point {
    private float x;
    private float y;
    private float z;
}
```

```idl
struct Point {
  x:float;
  y:float;
  z:float;
}
```

Superclass fields are included after the class's own fields. Structs may only contain scalars, enums, other structs and fixed-length arrays of those (`@FlatBufferFieldOptions(fieldType = FieldType.ARRAY, fixedLength = 4) float[] rotation` → `rotation:[float:4]`).

### Field options

`@FlatBufferFieldOptions` requires `fieldType`. Use the scalar or `STRING` types to override the mapped type, or `IDENT`, `ARRAY` or `MAP` to keep it.

Unsigned types (Java has no unsigned primitives):

```java
@FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
private byte flags;
@FlatBufferFieldOptions(fieldType = FieldType.USHORT)
private short port;
@FlatBufferFieldOptions(fieldType = FieldType.UINT)
private int count;
@FlatBufferFieldOptions(fieldType = FieldType.ULONG)
private long id;
```

```idl
  flags:ubyte;
  port:ushort;
  count:uint;
  id:ulong;
```

Binary data: `byte[]` maps to `[ubyte]`; `fieldType = FieldType.BYTE` gives `[byte]`:

```java
private byte[] payload;
@FlatBufferFieldOptions(fieldType = FieldType.BYTE)
private byte[] signedPayload;
```

```idl
  payload:[ubyte];
  signed_payload:[byte];
```

Attributes and field ids:

```java
@FlatBufferFieldOptions(fieldType = FieldType.STRING, fieldId = 0, required = true)
private String id;
@FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 1)
private int version;
```

```idl
  id:string (required, id: 0);
  version:int (id: 1);
```

Other options: `defaultValue`, `deprecated`, `key` (one per table, not deprecated), `hash` (`fnv1_32`, `fnv1a_32`, `fnv1_64`, `fnv1a_64`, on 32/64-bit integers or vectors of them), `nestedFlatbuffer` and `flexbuffer` (on `[ubyte]` fields), `forceAlign` (on vectors), `fixedLength` (in structs), `useFullName` (reference a type by its fully qualified Java name), and `attributes` (see [Custom attributes](#custom-attributes)). Ids are written only when every field in the table has one; they must be unique and contiguous from 0, and a union field also takes the id before its own.

### Documentation comments

`@FlatBufferComment(comment = "...")` on a class, field, enum constant, union member or RPC method is written as `///` lines, which `flatc` copies into the generated code. Each line of text becomes one `///` line:

```java
@FlatBufferComment(comment = "A player.\nStored by PlayerStore.")
@FlatBufferTable(rootType = true, originalOrder = true)
public class Player { ... }
```

```idl
/// A player.
/// Stored by PlayerStore.
table Player (original_order) {
```

Before 1.1.0 these were `//` comments, which `flatc` ignores.

### Optional scalars

A scalar table field that is not set reads as its default (`0` unless given). FlatBuffers' optional scalars (`= null`) read as null instead, which matches a boxed Java field. With `<optionalScalars>true</optionalScalars>`, boxed scalars (`Integer`, `Boolean`, `Long`, …) in tables are written that way, unless they have a `defaultValue` or are `required`; struct fields are never optional:

```java
private Integer rank;
private Boolean verified;
private int score;
```

```idl
  rank:int = null;
  verified:bool = null;
  score:int;
```

`defaultValue = "null"` makes a single field optional, including an enum field.

### Custom attributes

`attributes` on `@FlatBufferFieldOptions`, `@FlatBufferTable` and `@FlatBufferEnum` adds user-defined attributes, `"name"` or `"name: value"`, for your own tools to read from the schema. Each name is declared once with `attribute "name";`:

```java
@FlatBufferFieldOptions(fieldType = FieldType.INT, attributes = {"priority: 1"})
private int score;
```

```idl
attribute "priority";
...
  score:int (priority: 1);
```

Names must be identifiers and cannot be FlatBuffers' own attributes (such as `key`); use the annotation option for those.

### Table and struct attributes

`@FlatBufferTable(originalOrder = true)` keeps a table's fields in declaration order in the buffer (`original_order`). `@FlatBufferTable(value = STRUCT, forceAlign = 16)` raises a struct's alignment (`force_align`, a power of two):

```idl
struct Vec4 (force_align: 16) {
  x:float;
  y:float;
  z:float;
  w:float;
}
```

### Enums

Without a value field, constants are written in declaration order with implicit values. `enumType` sets the underlying type (`BYTE` by default):

```java
@FlatBufferEnum(enumType = EnumType.UINT)
public enum Color { RED, GREEN, BLUE }
```

```idl
enum Color : uint {
  RED,
  GREEN,
  BLUE,
}
```

For explicit values, mark one integral field with `@FlatBufferEnumTypeField`. Values are written in ascending order (unsigned types reinterpret Java's signed values, e.g. `(byte) 0xFF` is `255` for `UBYTE`):

```java
@FlatBufferEnum(enumType = EnumType.SHORT)
public enum Priority {
    HIGH((short) 30), LOW((short) 10);

    @FlatBufferEnumTypeField
    private final short weight;

    Priority(short weight) { this.weight = weight; }
}
```

```idl
enum Priority : short {
  LOW = 10,
  HIGH = 30,
}
```

A table field of enum type defaults to `0`. If the enum has no value `0`, as `Priority` here, give the field a default that is one of its constants, or `flatc` rejects the schema:

```java
@FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "LOW")
private Priority priority;
```

`@FlatBufferEnum(enumType = EnumType.UBYTE, bitFlags = true)` writes a `bit_flags` enum (unsigned types only): values are bit positions, so implicit values give 1, 2, 4, … and a field may default to `0`, meaning no flags:

```java
@FlatBufferEnum(enumType = EnumType.UBYTE, bitFlags = true)
public enum Permission {
    @FlatBufferComment(comment = "May read")
    READ,
    WRITE,
    ADMIN
}
```

```idl
enum Permission : ubyte (bit_flags) {
  /// May read
  READ,
  WRITE,
  ADMIN,
}
```

### Unions

Unions use `@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)`; each constant names a table, which is looked up in the union's namespace first and qualified when it is declared elsewhere. `@FlatBufferUnionMember(Table.class)` makes the constant an alias, so one table can appear under several names:

```java
@FlatBufferEnum(FlatBufferEnum.EnumStructureType.UNION)
public enum Position {
    @FlatBufferUnionMember(MarkerPosition.class)
    Start,
    PointPosition,
    @FlatBufferUnionMember(MarkerPosition.class)
    Finish
}
```

```idl
union Position {
  Start:MarkerPosition,
  PointPosition,
  Finish:MarkerPosition,
}
```

A union field adds a hidden `<name>_type` field, so no other field may have that name. Vectors of unions are supported by `flatc` only for C++.

### RPC services

`@FlatBufferRpcService` on an interface declares an `rpc_service`. Each abstract method taking one table and returning one becomes a method; mark a method `@FlatBufferIgnore` to leave it out. `flatc --grpc` generates code for it:

```java
@FlatBufferRpcService
public interface PlayerStore {
    @FlatBufferComment(comment = "Saves a player")
    PointPosition save(Player player);
}
```

```idl
rpc_service PlayerStore {
  /// Saves a player
  save(Player):PointPosition;
}
```

### Nested types

Classes and enums nested directly in an annotated table or struct are declared automatically, without their own annotation:

```java
@FlatBufferTable(rootType = true)
public class Knight {
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SHORT_SWORD")
    private Weapon weapon;

    @FlatBufferIgnore
    private String sessionToken;

    public enum Weapon { SHORT_SWORD, LONG_SWORD }
}
```

```idl
enum Weapon : byte {
  SHORT_SWORD,
  LONG_SWORD,
}

table Knight {
  weapon:Weapon = SHORT_SWORD;
}

root_type Knight;
```

Types are declared by their simple name (`Knight.Weapon` becomes `Weapon`). Mark a nested type `@FlatBufferIgnore` to leave it out; types nested more than one level deep are not declared automatically.

### Ignoring fields

`@FlatBufferIgnore` on a field leaves it out of the schema (`sessionToken` above).

### Namespaces

`@FlatBufferTable(namespace = "...")` and `@FlatBufferEnum(namespace = "...")` place one type in another namespace. A `namespace` statement is written only where the namespace changes, the default is restored before `root_type`, and references across namespaces (including `root_type`) are fully qualified.

### Maps

FlatBuffers has no map type. A `Map<K, V>` field becomes a vector of a generated table named `MapValueSet_<Owner>_<field>`, with the key written as `string`:

```idl
  scores:[MapValueSet_Account_scores];

table MapValueSet_Account_scores {
  key:string;
  value:int;
}
```

A map whose values are maps gets one table per level: `Map<String, Map<String, Integer>> levels` becomes `levels:[MapValueSet_Account_levels]`, whose `value` is `[MapValueSet_Account_levels_value]`.

---

## Sample output

From an annotated `Account` table and `Status` enum, with `fileIdentifier = ACCT` and `generateVersion = true`. This is 1.1.0 output; 1.0.1 writes the comment as `// Account state`, which also changes the hash:

```idl
// Aurkitu 1.1.0 automatically generated FlatBuffer schema
// @version: edf69fb7
namespace com.example;

file_identifier "ACCT";

/// Account state
enum Status : byte {
  ACTIVE,
  SUSPENDED,
  CLOSED,
}

table Account {
  id:long;
  display_name:string (required);
  level:int = 42;
  tags:[string];
  status:Status;
  scores:[MapValueSet_Account_scores];
}

table MapValueSet_Account_scores {
  key:string;
  value:int;
}

root_type Account;
```

Declarations are written as `include`s first, then the namespace, file identifier and extension, enums and unions, structs (each after the structs it contains), tables, generated map tables and `root_type`.

---

## Validation

With `validate` on (the default), problems are listed as comments at the end of the schema and logged as warnings. Set `failOnValidationIssues` to fail the build instead. Reported:

- Unsupported field types: `char`, nested vectors (`int[][]`, `List<List<T>>`), and JDK types that are not mapped. These fields are left out.
- References to types the schema does not declare. These fields are left out. This is not checked when `includes` are set, since included schemas are not read.
- Struct fields that are not scalars, enums, structs or fixed-length arrays of those. They are left out, and a struct left with no fields is left out too (`flatc` does not allow empty structs), with the fields that use it.
- A struct marked as the root type (`root_type` must be a table); no root type; more than one root type.
- Field ids on only some fields of a table, repeated ids, gaps, or a union field without the id before its own free (all ids are then left out).
- Attributes on the wrong kind of field: `required` on scalars, enums or struct fields; `deprecated` on struct fields; `key` on anything but a string, scalar, enum or struct (or, in a struct, a fixed-length array), on a deprecated field, or a second `key`; `hash` with an unknown algorithm or not on an integer (or vector of integers) of its width; `nested_flatbuffer` and `flexbuffer` not on `[ubyte]`, or `nested_flatbuffer` naming a table that is not declared. The attribute is left out.
- Defaults that `flatc` rejects: on strings, vectors, tables or struct fields, together with `required`, or not fitting the type (`300` on a `ubyte`, `abc` on an `int`). The default is left out. `= null` is valid on scalars and enums.
- `fixedLength` outside a struct (written as a vector), two fields written with the same name (`fooBar` and `foo_bar`), and a field clashing with a union's hidden `<name>_type` field.
- Enum problems: no values, duplicate or out-of-range values, more than one `@FlatBufferEnumTypeField`, a value field that cannot be read, table enum fields whose default is not an enum value, and `bit_flags` on a signed type or with a bit position that does not fit.
- Union members that are enums or unions, named `NONE` or twice, or (without `includes`) not declared; vectors of unions (C++ only).
- `original_order` on a struct, `force_align` on a table or not a power of two, and custom attribute names that are invalid or built in.
- RPC methods that do not take and return one declared table.
- Namespaces that are not dot-separated identifiers, two types with the same name in one namespace, and a `file_identifier` that is not 4 characters.

**Warnings** are listed after the issues and logged, but never fail the build. They flag schemas `flatc` accepts but that are risky or do not do what they say: inherited fields that existing data depends on (see [Schema evolution](#schema-evolution)), and `force_align` on a field that is not a vector, which `flatc` ignores (it is left out).

The rules are checked against `flatc` itself: a test compiles every field type, in a table and in a struct, with each attribute and each pair of attributes, and requires an issue exactly when `flatc` rejects the schema, and the written schema to compile every time.

`flatcPath` adds a full check by `flatc` itself.

---

## Schema evolution

FlatBuffers stores table fields by position, so a schema must change only in compatible ways ([Evolution](https://flatbuffers.dev/evolution/)): add new fields at the end, never remove or reorder fields (mark them `deprecated` instead), and do not change types or defaults. Aurkitu writes fields in Java declaration order, so the same rules apply to your Java classes:

- Add new fields after the existing ones; do not reorder or delete fields, or mark them `@FlatBufferIgnore` once data exists. Use `@FlatBufferFieldOptions(deprecated = true)` to retire a field.
- Inherited fields are written after the class's own fields, so adding a field to a subclass moves them. Aurkitu warns about tables with inherited fields and no field ids.
- `fieldId` on every field of a table fixes each field's position, so fields can then be reordered in Java safely.

---

## Limitations

- **Maps** need a small translation layer in clients (`MapValueSet_*` tables).
- **Includes** are written but not read, so types from included schemas are not resolved or validated.
- **One schema file** per execution; per-type namespaces are written into that file.
- **Field names**: consecutive capitals are split letter by letter (`userID` becomes `user_i_d`).
- **Map keys** are always written as `string`, whatever the Java key type.
- **Scanning** without `specifiedPackages` scans the whole compile classpath, which is slower.

---

## Compiling schemas with flatc

Download or build `flatc` from the [FlatBuffers](https://github.com/google/flatbuffers) project (25.x recommended):

```bash
flatc --java --gen-mutable -o target/aurkitu/output/java target/aurkitu/schemas/*.fbs
flatc --cpp -o target/aurkitu/output/cpp target/aurkitu/schemas/*.fbs
```

### Checking schemas with flatc

Set `flatcPath` and the plugin runs `flatc --binary --schema` on the generated schema, failing the build if `flatc` rejects it:

```xml
<configuration>
    <schemaName>user</schemaName>
    <flatcPath>/usr/local/bin/flatc</flatcPath>
</configuration>
```

Or with the exec plugin:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>check-schema</id>
            <phase>verify</phase>
            <goals><goal>exec</goal></goals>
            <configuration>
                <executable>flatc</executable>
                <arguments>
                    <argument>--binary</argument>
                    <argument>--schema</argument>
                    <argument>-o</argument>
                    <argument>${project.build.directory}/aurkitu/bfbs</argument>
                    <argument>${project.build.directory}/aurkitu/schemas/user.fbs</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Building Aurkitu

Requires JDK 17+.

```bash
mvn clean verify
```

`verify` runs the unit tests, then the end-to-end tests in `aurkitu-maven-plugin/src/it`. Each directory there is a real consumer project, built with [maven-invoker-plugin](https://maven.apache.org/plugins/maven-invoker-plugin/) against the freshly built plugin:

| Project | Covers |
|---------|--------|
| `kitchen-sink` | Every supported annotation and field attribute, enums, unions, structs, inheritance, nested types, maps, common types, includes, a per-type namespace |
| `settings-variants` | `snakeCaseFields=false`, `mapCommonTypes=false`, `ignoreStaticMembers=false`, `generateVersion`, validation comments |
| `fail-on-validation` | `failOnValidationIssues=true` fails the build |
| `flatc-path` | The `flatcPath` option (runs only when `FLATC_PATH` is set) |
| `dependency-models` | Annotated models in a dependency jar (two-module build) |

Each `verify.groovy` compares the generated schema with `expected/*.fbs` and, when `FLATC_PATH` points to a `flatc` binary, compiles it with `flatc --cpp --java`. Without `FLATC_PATH` that step is skipped locally; in CI (`CI=true`) it fails the build instead. If you change the output on purpose, update the expected file in the same commit. Use `-Dinvoker.skip` to skip these tests.

CI (`.github/workflows/main.yml`) downloads the official `flatc` binary (version in `FLATC_VERSION`), puts it on `PATH` and sets `FLATC_PATH`, so the end-to-end projects and the `FlatcValidationTest` unit test compile generated schemas with it.

### Coverage

JaCoCo is the single source of truth. `mvn verify` fails if **any class** drops below **80% line coverage or 80% branch coverage** (`jacoco.line.coverage.minimum` and `jacoco.branch.coverage.minimum` in the parent POM). The check is per class, so a well-tested module cannot hide an untested class.

Codecov, Coveralls and Codacy are mirrors and need these repository secrets:

| Secret | Tool | Without it |
|--------|------|------------|
| `CODECOV_TOKEN` | Codecov | The upload fails the job |
| `COVERALLS_REPO_TOKEN` | Coveralls | The upload fails the job |
| `CODACY_PROJECT_TOKEN` | Codacy Coverage Reporter | The step fails without failing the job (`continue-on-error`) |

Codacy's quality-grade badge comes from its static analysis and is independent of coverage.

### Publishing

Publishing a GitHub release runs `.github/workflows/publish.yml`, which builds the release tag and deploys both artifacts and the parent POM to Maven Central (signed, with sources and Javadoc). It needs the `MAVEN_USERNAME`, `MAVEN_PASSWORD`, `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE` secrets. The workflow can also be run manually on a tag. If the version is already on Maven Central, the run does nothing, so publishing the release page after a manual run is safe.

---

## What's new in 1.1.0 (unreleased)

- **Validation** of the FlatBuffers schema rules Aurkitu used to write without checking: `required`, defaults, struct-only and vector-only attributes, field ids, `key`, `hash`, `nested_flatbuffer`, duplicate field names, union `_type` clashes and more (see [Validation](#validation)). Invalid parts are reported and left out.
- **Warnings** for risks to existing data, starting with inherited fields in tables without field ids ([Schema evolution](#schema-evolution)).
- **Fix**: an optional enum field (`= null`) was reported as an invalid default.
- **Documentation comments**: `@FlatBufferComment` writes `///` comments, which `flatc` copies into generated code, and now also works on enum constants, union members and RPC methods. Output change: `//` comments become `///`.
- **New features**: optional scalars (`optionalScalars`), `bit_flags` enums, union aliases (`@FlatBufferUnionMember`), RPC services (`@FlatBufferRpcService`), custom attributes (`attributes`), `original_order` and struct `force_align`.
- **Coveralls** receives both modules' coverage.

## What's new in 1.0.1

1.0.0 generated an empty schema in real builds. 1.0.1 fixes that and corrects the generated output in many places, checked by end-to-end builds and `flatc`:

- **Classpath**: the project's compile classpath is scanned (1.0.0 scanned only the plugin's own).
- **Enums**: unsigned underlying types are kept (`UINT` was written as `int`); `@FlatBufferEnumTypeField` values are written (they were ignored).
- **Types**: `String` maps even with `mapCommonTypes=false`; more common types are mapped; `byte[]` maps to `[ubyte]` (it was `[byte]`); collections and maps are recognised by their real type, so a class named like `PlayerList` is no longer dropped.
- **Names and namespaces**: nested types are referenced by the name they are declared with; a per-type namespace no longer leaks into the declarations after it; `$` in comments and defaults is no longer rewritten.
- **Layout**: `include`s come first and structs are declared before their users, as `flatc` requires.
- **Restored from 0.0.x**: unannotated nested types of annotated tables are declared again, and the 0.0.x validation checks are back (see [Validation](#validation)).
- **`cacheEnabled`** compares the generated content: field changes are detected and validation always runs (1.0.0 hashed type names only and skipped validation on a hit). No `.fbs.cache` file is written.
- **`flatcPath`** runs `flatc --binary --schema` (1.0.0 ran `flatc --schema`, which names no output).
- **`@FlatBufferOptions`** is deprecated. It has never had an effect.

Output changes to expect when upgrading from 1.0.0: explicit enum values are written in ascending order; `byte[]` becomes `[ubyte]`; `@version` hashes change once (they no longer depend on the plugin version); new validation issues may be reported, and fail the build with `failOnValidationIssues`.

---

## Migration from 0.0.x

### Coordinates and packages

| | 0.0.x | 1.0.x |
|---|-------|-------|
| groupId | `com.michaelhradek` | `io.github.mhradek` |
| Java packages | `com.michaelhradek.aurkitu.*` | `io.github.mhradek.aurkitu.*` |
| Java baseline | 8 | 17 |
| Command-line properties | `aurkitu-maven-plugin.*` | `aurkitu.*` |

### Plugin parameters

| 0.0.x | 1.0.x |
|-------|-------|
| `schemaName`, `schemaNamespace`, `outputDirectory`, `fileExtension`, `generateVersion`, `ignoreStaticMembers` | Unchanged |
| `schemaFileIdentifier` | `fileIdentifier` |
| `schemaIncludes` | `includes` |
| `validateSchema` | `validate` |
| `useSchemaCaching` (skip if the file exists) | `cacheEnabled` (rewrite only when the content changes) |
| `specifiedDependencies` (artifacts) | `specifiedPackages` (packages); dependencies on the compile classpath are scanned |
| `namespaceOverrideMap` | Removed; use `namespace` on `@FlatBufferTable` / `@FlatBufferEnum` |
| `consolidatedSchemas` | Removed; one schema per execution |
| — | New: `snakeCaseFields`, `mapCommonTypes`, `failOnValidationIssues`, `flatcPath` |

### Behaviour

| Area | 0.0.x | 1.0.x |
|------|-------|-------|
| Field names | As declared (camelCase) | snake_case by default; `snakeCaseFields=false` keeps them as declared |
| Map tables | `MapValueSet_<Owner>_<field>` | Same; the field part follows `snakeCaseFields` |
| Inheritance | Superclass fields included | Same |
| Nested types | Declared automatically | Same (1.0.0 dropped this; restored in 1.0.1) |
| Unions | Supported | Supported |
| `required`, `deprecated`, `fieldId`, `key`, `hash`, `nested_flatbuffer`, `flexbuffer`, `force_align`, `fixedLength` | Not available | Available on `@FlatBufferFieldOptions` |
| Common JDK types (`Date`, `UUID`, …) | Not mapped | Mapped to `string` (`mapCommonTypes`) |
| `byte[]` | `[byte]` | `[ubyte]` |
| Validation | Comments in the schema | Comments plus warnings; optional `failOnValidationIssues` and `flatcPath` |
| `@FlatBufferOptions` | No effect | Deprecated, no effect |

### Steps

1. Update the dependency and plugin coordinates to `io.github.mhradek` and version `1.0.1`.
2. Change imports from `com.michaelhradek.aurkitu.*` to `io.github.mhradek.aurkitu.*`.
3. Rename plugin parameters as above.
4. Set `<snakeCaseFields>false</snakeCaseFields>` if clients depend on camelCase field names.
5. Regenerate the schemas and review the validation comments.

---

## Roadmap

| Priority | Item | Status |
|----------|------|--------|
| Medium | Published Javadoc site | Javadoc jars are on Maven Central |
| Low | Schema include resolution | Includes are written, not read |
| Low | Multiple schema files (one per namespace) | One file per execution |
| Low | Acronym-aware snake_case (`userID` → `user_id`) | Changes field names, so opt-in |
| Low | Typed map keys and a `(key)` attribute on `MapValueSet_*` keys | Keys are written as `string` |
| Low | Remove deprecated `@FlatBufferOptions` | Planned for 2.0 |

## License

MIT
