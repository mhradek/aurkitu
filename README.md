# Aurkitu 1.3.0

[![Latest release](https://img.shields.io/github/release/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/releases/latest)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-maven-plugin.svg?label=maven%20plugin)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-maven-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.mhradek.aurkitu.svg?label=gradle%20plugin)](https://plugins.gradle.org/plugin/io.github.mhradek.aurkitu)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-annotations.svg?label=annotations)](https://central.sonatype.com/artifact/io.github.mhradek/aurkitu-annotations)
[![Build Status](https://github.com/mhradek/aurkitu/actions/workflows/main.yml/badge.svg)](https://github.com/mhradek/aurkitu/actions)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/42b23d745afc4ef1b1102807752e8a38)](https://app.codacy.com/gh/mhradek/aurkitu/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![codecov](https://codecov.io/gh/mhradek/aurkitu/branch/master/graph/badge.svg)](https://codecov.io/gh/mhradek/aurkitu)
[![Coverage Status](https://coveralls.io/repos/github/mhradek/aurkitu/badge.svg?branch=master&v=2)](https://coveralls.io/github/mhradek/aurkitu?branch=master)
[![CodeQL](https://github.com/mhradek/aurkitu/actions/workflows/codeql.yml/badge.svg)](https://github.com/mhradek/aurkitu/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/mhradek/aurkitu/badge)](https://scorecard.dev/viewer/?uri=github.com/mhradek/aurkitu)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/16496/badge)](https://www.bestpractices.dev/projects/16496)
[![javadoc](https://img.shields.io/maven-central/v/io.github.mhradek/aurkitu-annotations.svg?label=javadoc)](https://javadoc.io/doc/io.github.mhradek/aurkitu-annotations)
[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/stargazers)

**Aurkitu** (Basque for “to find”) generates FlatBuffers IDL (`.fbs`) schemas from annotated Java models. This keeps your server-side models as the single source of truth — especially useful when models carry storage annotations (e.g. DynamoDB) that must remain authoritative.

- **Maven or Gradle**: a [Maven plugin](#maven-plugin) and a [Gradle plugin](#gradle-plugin) with the same options, built on the same generator, so both write the same schema byte for byte.
- **The FlatBuffers schema language**: tables, structs, enums (including `bit_flags`), unions, maps, inheritance, field ids and attributes, optional scalars, RPC services, namespaces, includes and documentation comments.
- **Schemas that compile**: [validation](#validation) of the rules `flatc` enforces, tested against `flatc` itself, with an optional check by `flatc` in the build.
- **Your models as they are**: classes of any visibility, in your project or in a dependency, scanned from the compile classpath. Java 17 or later; Kotlin JVM classes work too.
- **Any client language**: `flatc` turns the schema into code for [C#, Swift, TypeScript, Go, Rust and more](#using-the-schema-from-other-languages), so clients share your Java models.

> **Upgrading from 1.1.x?** Field names with consecutive capitals change (`userID` is now `user_id`); see [What's new in 1.2.0](#whats-new-in-120).
>
> **Upgrading from 1.1.0?** Non-public classes are now scanned; see [What's new in 1.1.1](#whats-new-in-111).
>
> **Upgrading from 1.0.1?** See [What's new in 1.1.0](#whats-new-in-110) for the output changes to expect.
>
> **Upgrading from 1.0.0?** 1.0.0 scanned the plugin's own classpath instead of your project's, so it generated an empty schema in real builds. See [What's new in 1.0.1](#whats-new-in-101).
>
> **Upgrading from 0.0.x?** See [Migration from 0.0.x](#migration-from-00x).

---

## Quick start

1. Add the annotations and the plugin to your build.

   **Maven** (`pom.xml`):

   ```xml
   <dependencies>
       <dependency>
           <groupId>io.github.mhradek</groupId>
           <artifactId>aurkitu-annotations</artifactId>
           <version>1.3.0</version>
       </dependency>
   </dependencies>

   <build>
       <plugins>
           <plugin>
               <groupId>io.github.mhradek</groupId>
               <artifactId>aurkitu-maven-plugin</artifactId>
               <version>1.3.0</version>
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
       </plugins>
   </build>
   ```

   **Gradle** (`build.gradle.kts`):

   ```kotlin
   plugins {
       java
       id("io.github.mhradek.aurkitu") version "1.3.0"
   }

   dependencies {
       implementation("io.github.mhradek:aurkitu-annotations:1.3.0")
   }

   aurkitu {
       schemaName = "user"
       schemaNamespace = "com.example.flatbuffers"
       // Optional but recommended: limits scanning to your model packages
       specifiedPackages = listOf("com.example.models")
   }
   ```

   The Groovy DSL (`build.gradle`) is the same with `id 'io.github.mhradek.aurkitu' version '1.3.0'` and lists in square brackets: `specifiedPackages = ['com.example.models']`.

2. Annotate your models:

   ```java
   import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

   @FlatBufferTable(rootType = true)
   public class User {
       private long id;
       private String name;
   }
   ```

   Classes, enums and fields can have any visibility (`public`, package-private, `protected` or `private`): the annotation is what puts a type in the schema, and a model used only inside your code can stay package-private.

3. Build: `mvn package` writes `target/aurkitu/schemas/user.fbs`, and `gradle build` writes `build/aurkitu/schemas/user.fbs`:

   ```idl
   // Aurkitu 1.3.0 automatically generated FlatBuffer schema
   namespace com.example.flatbuffers;

   table User {
     id:long;
     name:string;
   }

   root_type User;
   ```

Then compile it with `flatc` for the languages you need: see [Compiling schemas with flatc](#compiling-schemas-with-flatc).

---

## Maven plugin

The `build-schema` goal runs in the `process-classes` phase, after your classes are compiled, so `mvn compile` does not run it but `mvn process-classes`, `mvn package` and later phases do. It scans the project's compile classpath: its own classes and its compile-scope dependencies (including `provided`), so annotated models can live in a dependency jar.

Options are set in `<configuration>` (see [Configuration options](#configuration-options)), or on the command line with their property, which a `<configuration>` value overrides: `mvn package -Daurkitu.validate=false`.

To write several schemas from one project, add an `<execution>` per schema, each with its own `<id>` and `<configuration>`; options in the plugin's own `<configuration>` apply to all of them:

```xml
<configuration>
    <schemaNamespace>com.example.flatbuffers</schemaNamespace>
</configuration>
<executions>
    <execution>
        <id>models</id>
        <goals><goal>build-schema</goal></goals>
        <configuration>
            <schemaName>models</schemaName>
            <specifiedPackages><specifiedPackage>com.example.models</specifiedPackage></specifiedPackages>
        </configuration>
    </execution>
    <execution>
        <id>events</id>
        <goals><goal>build-schema</goal></goals>
        <configuration>
            <schemaName>events</schemaName>
            <specifiedPackages><specifiedPackage>com.example.events</specifiedPackage></specifiedPackages>
        </configuration>
    </execution>
</executions>
```

---

## Gradle plugin

> New in 1.3.0, on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.mhradek.aurkitu). Tested with Gradle 8.14 and 9.9, and on Java 17, 21 and 25.

The plugin adds the `aurkitu { }` block and the `generateFlatBuffersSchema` task. With a Java or Kotlin JVM plugin applied, the task scans the `main` source set's classes and its compile classpath, so annotated classes in dependencies and in other projects of the build are found, as with Maven. Without one there is nothing to scan, and the task fails saying so.

**When it runs**: `assemble`, and so `build`, runs it; so does `gradle generateFlatBuffersSchema` on its own. The task is cacheable and works with the configuration cache: it runs again only when the scanned classes or the options change, and a clean build or another checkout can take the schema from the [build cache](https://docs.gradle.org/current/userguide/build_cache.html).

**Options**: the `aurkitu { }` block takes every [configuration option](#configuration-options), with the same names and defaults as the Maven plugin, except two:

| Option | Maven | Gradle |
|--------|-------|--------|
| `schemaName` | Required | Optional: defaults to the project's name |
| `outputDirectory` | `target/aurkitu/schemas` | `build/aurkitu/schemas` |

A test in this repository checks that the two plugins' options and defaults stay the same.

**On the command line, or in `gradle.properties`**, every option can be set with a Gradle property named as the Maven plugin's property: `gradle build -Paurkitu.validate=false`, `-Paurkitu.outputDir=schemas`, or `-Paurkitu.specifiedPackages=com.a,com.b` for a list (comma-separated). As in Maven, a value set in the build script wins over the property.

**Several schemas from one project**: register more tasks of the plugin's type. Each takes its options from `aurkitu { }` unless it sets its own, scans the same classes, writes to `build/aurkitu/<task name>` (tasks sharing an output directory cannot be cached) and runs with `assemble`:

```kotlin
tasks.register<io.github.mhradek.aurkitu.gradle.GenerateFlatBuffersSchema>("generateEventsSchema") {
    schemaName = "events"
    specifiedPackages = listOf("com.example.events")
}
```

A task can scan other classes than `main`'s with `classpath.setFrom(...)`.

**Using the schema in another task**: take it from the task's output, so Gradle runs the schema task first and reruns yours when the schema changes. For example, C# code from `flatc` (see [Compiling schemas with flatc](#compiling-schemas-with-flatc)):

```kotlin
tasks.register<Exec>("generateCSharp") {
    val schema = tasks.named<io.github.mhradek.aurkitu.gradle.GenerateFlatBuffersSchema>("generateFlatBuffersSchema")
        .flatMap { it.outputDirectory.file("user.fbs") }
    val out = layout.buildDirectory.dir("generated/csharp")
    inputs.file(schema)
    outputs.dir(out)
    executable = "flatc"
    argumentProviders.add(CommandLineArgumentProvider {
        listOf("--csharp", "-o", out.get().asFile.path, schema.get().asFile.path)
    })
}
```

**Kotlin models**: Kotlin classes are scanned like Java ones; a property's backing field is the schema field (`data class Order(val orderId: Long)` gives `order_id:long`). Nullability (`String?`) is not reflected in the schema.

**Java versions**: the generator runs in Gradle's JVM. If your models are compiled for a newer Java than the one running Gradle (a [toolchain](https://docs.gradle.org/current/userguide/toolchains.html) newer than Gradle's JVM) and use `@FlatBufferEnumTypeField`, the build fails, because the enum values cannot be read; see [Troubleshooting](#troubleshooting).

---

## Configuration options

The same options in both plugins: in Maven's `<configuration>` or Gradle's `aurkitu { }` block, or on the command line with the property (`-Daurkitu.validate=false` for Maven, `-Paurkitu.validate=false` for Gradle).

| Option | Property | Default | Description |
|--------|----------|---------|-------------|
| `schemaName` | `aurkitu.schemaName` | Maven: — (required); Gradle: the project's name | Output file name stem (`<schemaName>.fbs`) |
| `schemaNamespace` | `aurkitu.schemaNamespace` | `generated.flatbuffers` | Default FlatBuffers namespace |
| `outputDirectory` | `aurkitu.outputDir` | Maven: `target/aurkitu/schemas`; Gradle: `build/aurkitu/schemas` | Destination directory |
| `specifiedPackages` | `aurkitu.specifiedPackages` | all packages | Packages to scan (and their subpackages) |
| `validate` | `aurkitu.validate` | `true` | Report [validation](#validation) issues as comments at the end of the schema |
| `failOnValidationIssues` | `aurkitu.failOnValidationIssues` | `false` | Fail the build when there are validation issues |
| `ignoreStaticMembers` | `aurkitu.ignoreStaticMembers` | `true` | Skip static fields |
| `snakeCaseFields` | `aurkitu.snakeCaseFields` | `true` | Write field names in snake_case (`displayName` → `display_name`) |
| `snakeCaseAcronyms` | `aurkitu.snakeCaseAcronyms` | `true` | With `snakeCaseFields`, keep a run of capitals as one word: `userID` → `user_id`, `XMLParser` → `xml_parser`, `userIDs` → `user_ids`. Set to `false` for the 1.0/1.1 names (`user_i_d`, `x_m_l_parser`). Since 1.2.0. |
| `mapCommonTypes` | `aurkitu.mapCommonTypes` | `true` | Map `Date`, `UUID`, `URL`, `URI`, `BigDecimal`, `BigInteger` and `java.time` types (`Instant`, `LocalDate`, `LocalDateTime`, `LocalTime`, `OffsetDateTime`, `ZonedDateTime`, `Duration`) to `string`. `String` is always mapped. |
| `fileIdentifier` | `aurkitu.fileIdentifier` | — | Emit `file_identifier "..."` (must be 4 characters) |
| `fileExtension` | `aurkitu.fileExtension` | — | Emit `file_extension "..."` |
| `includes` | `aurkitu.includes` | — | Schema files to `include` (a property takes a comma-separated list, as does `specifiedPackages`) |
| `generateVersion` | `aurkitu.generateVersion` | `false` | Emit `// @version: <hash>`, a hash of the schema body. The header and validation comments are excluded, so it changes only when the schema does. |
| `flatcPath` | `aurkitu.flatcPath` | — | Path to a `flatc` binary. When set (and `validate` is on), the schema is checked with `flatc --binary --schema` and the build fails if `flatc` rejects it. |
| `cacheEnabled` | `aurkitu.cacheEnabled` | `false` | Only rewrite the schema file when its content changes, keeping its timestamp for downstream steps. The schema is still generated and validated every time. |
| `optionalScalars` | `aurkitu.optionalScalars` | `false` | Write boxed Java scalars (`Integer`, `Boolean`, …) in tables as [optional scalars](#optional-scalars) (`= null`). Since 1.1.0. |
| `typedMapKeys` | `aurkitu.typedMapKeys` | `false` | Write [map](#maps) keys with their own type, marked `(key)`, instead of always `string`. Off by default because it changes existing map tables. Since 1.2.0. |

Both plugins scan the project's compiled classes and its compile classpath, so annotated models can live in the project itself or in a dependency.

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

A scalar table field that is not set reads as its default (`0` unless given). FlatBuffers' optional scalars (`= null`) read as null instead, which matches a boxed Java field. With `optionalScalars` on, boxed scalars (`Integer`, `Boolean`, `Long`, …) in tables are written that way, unless they have a `defaultValue` or are `required`; struct fields are never optional:

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

Types are declared by their simple name (`Knight.Weapon` becomes `Weapon`). This includes `private` nested classes, so mark a nested helper class that is not part of the schema `@FlatBufferIgnore` to leave it out; types nested more than one level deep are not declared automatically.

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

With `typedMapKeys`, the key keeps its Java type and is marked `(key)`, so generated code can look entries up by key (build the vector sorted, e.g. with `createSortedVectorOfTables` in Java):

```idl
  names_by_id:[MapValueSet_Account_names_by_id];

table MapValueSet_Account_names_by_id {
  key:int (key);
  value:string;
}
```

Keys can be strings, boxed scalars, enums, structs and (with `mapCommonTypes`) the common types, which are written as `string`. A map keyed by a collection, map, array or any other type is reported and left out. A table key keeps its type but cannot be the `(key)`.

A map key or value whose enum has no `0` value gets the enum's lowest value as its default (`key:Status = ACTIVE (key);`), because `flatc` requires a table enum field's default to be one of its values and the generated fields cannot be annotated.

---

## Sample output

From an annotated `Account` table and `Status` enum, with `fileIdentifier = ACCT` and `generateVersion = true`. Every version since 1.1.0 writes this schema for this model; only the header names the version. (1.0.1 wrote the comment as `// Account state`, which also changed the hash.)

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
- `fixedLength` outside a struct (written as a vector), two fields written with the same name (`fooBar` and `foo_bar` with `snakeCaseFields`), and a field clashing with a union's hidden `<name>_type` field. The later field is left out.
- Enum problems: no values, duplicate or out-of-range values, more than one `@FlatBufferEnumTypeField`, a value field that cannot be read, table enum fields whose default is not an enum value, and `bit_flags` on a signed type or with a bit position that does not fit. A default that is not a value is left out when the enum has a `0` value (the field then defaults to it).
- **Kept for you to fix:** a table field of an enum with no `0` value, with no `defaultValue` or one that is not a value. Without a default the field would be `0`, which is not a value of the enum, and choosing one for you would change what an absent field means. The schema does not compile until you set `defaultValue` to one of the enum's values. This is the one issue whose field is not left out. (Generated map tables are different: their enum key or value gets the enum's lowest value, because a map entry always has both.)
- Union members that are enums or unions, named `NONE` or twice, or (without `includes`) not declared; vectors of unions (C++ only).
- `original_order` on a struct, `force_align` on a table or not a power of two, and custom attribute names that are invalid or built in.
- RPC methods that do not take and return one declared table.
- Namespaces that are not dot-separated identifiers, two types with the same name in one namespace, and a `file_identifier` that is not 4 characters.

**Warnings** are listed after the issues and logged, but never fail the build. They flag schemas `flatc` accepts but that are risky or do not do what they say: inherited fields that existing data depends on (see [Schema evolution](#schema-evolution)), and `force_align` on a field that is not a vector, which `flatc` ignores (it is left out).

The rules are checked against `flatc` itself: a test compiles every field type, in a table and in a struct, with each attribute and each pair of attributes, and requires an issue exactly when `flatc` rejects the schema, and the written schema to compile in every case except the enum default above.

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
- **One schema file** per Maven execution or Gradle task; per-type namespaces are written into that file. A project can have several (see [Maven plugin](#maven-plugin) and [Gradle plugin](#gradle-plugin)).
- **Field names**: a run of capitals is one word (`userID` becomes `user_id`). With `snakeCaseAcronyms=false`, every capital starts a word (`user_i_d`), as in 1.0 and 1.1.
- **Map keys** are written as `string` unless `typedMapKeys` is set.
- **Scanning** without `specifiedPackages` scans the whole compile classpath, which is slower, and picks up annotated classes in dependencies you may not want in the schema.
- **Kotlin nullability** is not reflected: `String?` and `String` are both `string`.

---

## Compiling schemas with flatc

Download or build `flatc` from the [FlatBuffers](https://github.com/google/flatbuffers) project (25.x recommended), then run it on the schema the build wrote (`target/aurkitu/schemas` with Maven, `build/aurkitu/schemas` with Gradle):

```bash
flatc --java --gen-mutable -o target/aurkitu/output/java target/aurkitu/schemas/*.fbs
flatc --cpp -o target/aurkitu/output/cpp target/aurkitu/schemas/*.fbs
```

To run `flatc` as part of a Gradle build, see the `generateCSharp` example in [Gradle plugin](#gradle-plugin).

### Using the schema from other languages

The schema is plain FlatBuffers IDL, so `flatc` generates code from it for every language FlatBuffers supports. Your Java models stay the single source of truth, and a C# game client, a Swift or Kotlin app, a TypeScript web client or a Go or Rust service reads and writes the same buffers:

| Language | `flatc` option | Notes |
|----------|----------------|-------|
| C# (.NET, Unity) | `--csharp` | Add `--gen-object-api` for plain classes to build buffers from |
| C++ | `--cpp` | `--gen-object-api` for the object API; `--grpc` for RPC services |
| Go | `--go` | `--go-namespace` sets the package |
| Rust | `--rust` | |
| Swift | `--swift` | |
| Kotlin | `--kotlin` | Or `--java`, which Kotlin uses too |
| TypeScript / JavaScript | `--ts` | |
| Python | `--python` | |
| Dart | `--dart` | |

For example, from the schema a Maven build wrote:

```bash
flatc --csharp --gen-object-api -o client/Generated target/aurkitu/schemas/user.fbs
flatc --swift -o ios/Generated target/aurkitu/schemas/user.fbs
flatc --ts -o web/src/generated target/aurkitu/schemas/user.fbs
```

Field names are written in snake_case (`snakeCaseFields`), the FlatBuffers style; `flatc` turns them into each language's own convention (`UserId` in C#, `userId` in Swift and TypeScript). The binary format is the same whichever language reads it, and `file_identifier` (`fileIdentifier`) lets a reader check a buffer is of the expected type.

To keep clients in step, publish the `.fbs` with your service (for example as a build artifact or in a shared repository) and run `flatc` in each client's build. `generateVersion` adds a hash that changes when the schema does, so a client can tell it has an older copy. The [Schema evolution](#schema-evolution) rules say which model changes old clients can still read.

### Checking schemas with flatc

Set `flatcPath` and the plugin runs `flatc --binary --schema` on the generated schema, failing the build if `flatc` rejects it:

```xml
<configuration>
    <schemaName>user</schemaName>
    <flatcPath>/usr/local/bin/flatc</flatcPath>
</configuration>
```

```kotlin
aurkitu {
    flatcPath = "/usr/local/bin/flatc"
}
```

Or with `-Paurkitu.flatcPath=...` / `-Daurkitu.flatcPath=...`, so each machine can use its own `flatc`. With Maven, the exec plugin can also run it:

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

## Troubleshooting

**The schema is empty, or types are missing.** Check `specifiedPackages`: it lists packages to scan, including their subpackages, and a type outside them is not declared (fields that use it are then reported as referring to an undeclared type). A class needs `@FlatBufferTable` or `@FlatBufferEnum`, or must be nested directly in one that has it. With Maven, the goal must run after compilation (`process-classes` or later); with Gradle, the project needs a Java or Kotlin JVM plugin.

**There are validation comments at the end of the schema.** Each says what was left out and why; see [Validation](#validation). They are also logged as warnings. To make them fail the build, set `failOnValidationIssues`.

**`flatc` rejects the schema.** The usual cause is a table field whose enum has no `0` value and no `defaultValue`; Aurkitu reports it but cannot choose a default for you (see [Enums](#enums)). Set `flatcPath` to have the build catch this.

**"Cannot read the @FlatBufferEnumTypeField values of ...: it is compiled for Java N, but the build runs on Java M".** The enum's values have to be read from the class, which the JVM running the build cannot load. Run the build on Java N or later (Maven: run `mvn` with that JDK; Gradle: run Gradle with it, for example with `org.gradle.java.home` in `gradle.properties`), or compile the models for Java M. Up to 1.3.0, these enums were written with implicit values (`0, 1, …`) instead, with only a validation comment.

**Gradle: "nothing to scan; the task's classpath is empty".** Apply the `java` (or `java-library`, or Kotlin JVM) plugin, or give the task a `classpath`.

**Field names changed after upgrading to 1.2.0.** Names with consecutive capitals (`userID`) are written `user_id`, not `user_i_d`. Set `snakeCaseAcronyms` to `false` to keep the old names; see [What's new in 1.2.0](#whats-new-in-120).

---

## Building Aurkitu

Requires JDK 17+.

```bash
mvn clean verify
```

The modules:

| Module | What it is |
|--------|------------|
| `aurkitu-annotations` | The annotations users put on their models |
| `aurkitu-core` | The schema generator: scans the classes, builds the schema model, validates and writes it, and runs `flatc`. No dependency on Maven or Gradle |
| `aurkitu-maven-plugin` | The `build-schema` goal, a thin wrapper around the core |
| `aurkitu-gradle-plugin` | The Gradle plugin, also a thin wrapper around the core. A separate Gradle build (see below) |

The Gradle plugin is built with Gradle (8.14 or later; CI and releases use 9.9), against `aurkitu-core` of the same version from the local Maven repository, so install the Maven modules first:

```bash
mvn -B install -DskipTests -Dinvoker.skip
```

```bash
cd aurkitu-gradle-plugin && gradle build
```

Its version is read from the parent POM. `gradle build` runs functional tests ([TestKit](https://docs.gradle.org/current/userguide/test_kit.html)), real Gradle builds that apply the plugin:

- **Every Maven end-to-end project** in `aurkitu-maven-plugin/src/it`, built with Gradle from its POM's plugin configuration: it must succeed or fail as the Maven build does, and write the same schema byte for byte. A new Maven project there is covered automatically.
- **Option parity**: every `build-schema` parameter, read from the Maven plugin's descriptor, must be an `aurkitu { }` option with the same name and default, and the other way round.
- **The plugin as published**: it is published to a local repository and resolved by id and version, as from the Gradle Plugin Portal, so a published POM that does not resolve fails the build.
- The build cache (including a copy of the project at another path), the configuration cache, up-to-date checks, Gradle-property overrides, several schema tasks in one project, a Kotlin JVM project, `flatc`, and a build without a Java plugin.

It also runs unit tests ([ProjectBuilder](https://docs.gradle.org/current/javadoc/org/gradle/testfixtures/ProjectBuilder.html)) of the plugin's wiring and the task's action, with the same per-class coverage gate as the Maven modules. CI runs it with Gradle 8.14 on Java 17, and Gradle 9.9 on Java 17, 21 and 25.

`verify` runs the unit tests, then the end-to-end tests in `aurkitu-maven-plugin/src/it`. Each directory there is a real consumer project, built with [maven-invoker-plugin](https://maven.apache.org/plugins/maven-invoker-plugin/) against the freshly built plugin:

| Project | Covers |
|---------|--------|
| `kitchen-sink` | Every supported annotation and field attribute, enums, unions, structs, inheritance, nested types, maps (typed keys, enum keys and values), acronym snake_case, common types, includes, a per-type namespace |
| `settings-variants` | `snakeCaseFields=false`, `mapCommonTypes=false`, `ignoreStaticMembers=false`, `generateVersion`, validation comments |
| `fail-on-validation` | `failOnValidationIssues=true` fails the build |
| `flatc-path` | The `flatcPath` option (runs only when `FLATC_PATH` is set) |
| `dependency-models` | Annotated models in a dependency jar (two-module build) |

Each `verify.groovy` compares the generated schema with `expected/*.fbs` and, when `FLATC_PATH` points to a `flatc` binary, compiles it with `flatc --cpp --java`. Without `FLATC_PATH` that step is skipped locally; in CI (`CI=true`) it fails the build instead. If you change the output on purpose, update the expected file in the same commit. Use `-Dinvoker.skip` to skip these tests.

CI (`.github/workflows/main.yml`) downloads the official `flatc` binary (version in `FLATC_VERSION`), puts it on `PATH` and sets `FLATC_PATH`, so the end-to-end projects and the `FlatcValidationTest` unit test compile generated schemas with it.

### Coverage

JaCoCo is the single source of truth. `mvn verify` fails if **any class** drops below **80% line coverage or 80% branch coverage** (`jacoco.line.coverage.minimum` and `jacoco.branch.coverage.minimum` in the parent POM). The check is per class, so a well-tested module cannot hide an untested class. The Gradle plugin's build applies the same rule to its unit tests (`jacocoTestCoverageVerification`).

Codecov, Coveralls and Codacy are mirrors and need these repository secrets:

| Secret | Tool | Without it |
|--------|------|------------|
| `CODECOV_TOKEN` | Codecov | The upload fails the job |
| `COVERALLS_REPO_TOKEN` | Coveralls | The upload fails the job |
| `CODACY_PROJECT_TOKEN` | Codacy Coverage Reporter | The step fails without failing the job (`continue-on-error`) |

Codacy's quality-grade badge comes from its static analysis and is independent of coverage.

### Code quality checks

| Check | Where | What it does |
|-------|-------|--------------|
| Maven Enforcer | every build | Requires Java 17+ and Maven 3.6.3+, a pinned version for every plugin (so a build cannot change or downgrade silently), and convergent dependency versions |
| API compatibility ([japicmp](https://siom79.github.io/japicmp/)) | every build of `aurkitu-annotations` | Compares the annotations with the last release (`japicmp.baseline`). A binary or source incompatible change fails the build, and so does a change that needs a newer version number than the project has (semantic versioning: a new annotation option needs a minor version) |
| [SpotBugs](https://spotbugs.github.io/) with [FindSecBugs](https://find-sec-bugs.github.io/) | every build (`verify`) | Bug patterns PMD does not cover, and security rules. A finding fails the build; the few excluded on purpose are listed, with reasons, in `spotbugs-exclude.xml` |
| Mutation testing ([pitest](https://pitest.org/)) | weekly (`.github/workflows/mutation.yml`), or `mvn -Ppitest -pl aurkitu-core test-compile org.pitest:pitest-maven:mutationCoverage` | Changes the generator's code in small ways and checks a test fails each time. The run fails below 90% of mutants killed; the report is attached to the run |
| Reproducible builds | every build | `project.build.outputTimestamp` fixes the timestamps in the jars, so building the same commit twice gives byte-identical artifacts |
| Java 17, 21 and 25 | CI | The Maven build and every test run on each, and so does the Gradle plugin on Gradle 9.9 (plus Gradle 8.14 on Java 17), so newer JDKs keep working |
| [CodeQL](https://codeql.github.com/) | CI and weekly | GitHub's security and quality analysis; findings appear under Security → Code scanning |
| [OpenSSF Scorecard](https://scorecard.dev/) | weekly and on push | Scores the repository's security practices (the Scorecard badge) |
| Dependabot | weekly | Opens grouped PRs for Maven, the Gradle plugin's build and GitHub Actions. ClassGraph major versions are skipped (6.0 removed APIs the plugin uses) |
| Pinned actions | every workflow | Each GitHub Action is pinned to a commit (`@<sha> # vX.Y.Z`), so a moved tag cannot change a build; Dependabot updates the pins |
| Least-privilege tokens | every workflow | The `GITHUB_TOKEN` is read-only unless a job needs more (code scanning uploads, attaching release assets) |
| Codacy, Codecov, Coveralls | CI | Static analysis and coverage reports (see [Coverage](#coverage)) |

### Publishing

Publishing a GitHub release runs `.github/workflows/publish.yml`, which builds the release tag and deploys the annotations, the core, the Maven plugin and the parent POM to Maven Central (signed, with sources and Javadoc). Before deploying, it also builds and tests the Gradle plugin, so a broken Gradle plugin stops the release before anything reaches Maven Central. A second job then publishes the Gradle plugin to the [Gradle Plugin Portal](https://plugins.gradle.org/), with the same version, using the `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` repository secrets (an API key of the portal account that owns `io.github.mhradek`). Without them, or without `aurkitu-core` of that version on Maven Central, that job fails. A version already on the portal is skipped, so re-running either job is safe. It needs the `MAVEN_USERNAME`, `MAVEN_PASSWORD`, `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE` secrets. The workflow can also be run manually on a tag. If the version is already on Maven Central, the run does nothing, so publishing the release page after a manual run is safe. Publish the release page only after a manual run has finished, so the two runs do not both upload.

When a release is published, `.github/workflows/release-assets.yml` waits for it on Maven Central and attaches the jars, POMs and their `.asc` signatures to the GitHub release. It then rebuilds the tag and compares the result with Maven Central byte for byte. For the files that match, it attaches [SLSA](https://slsa.dev/) build provenance, `aurkitu-<version>.intoto.jsonl`, which you can check with [slsa-verifier](https://github.com/slsa-framework/slsa-verifier):

```bash
slsa-verifier verify-artifact aurkitu-maven-plugin-<version>.jar \
  --provenance-path aurkitu-<version>.intoto.jsonl \
  --source-uri github.com/mhradek/aurkitu --source-tag <version>
```

Releases up to 1.1.1 were built before reproducible builds, so they get no provenance. The workflow can also be run manually with a tag, e.g. to add assets to an older release.

Before tagging a release, set `project.build.outputTimestamp` in the parent POM to the release date (any fixed value works; the date documents when it was cut).

After a release, move the POMs to the next `-SNAPSHOT` version and set `japicmp.baseline` to the version just released. The build enforces this: with the project version equal to the baseline, japicmp would compare the annotations with themselves.

---

## What's new since 1.3.0 (unreleased)

- **Gradle: options from Gradle properties**: `-Paurkitu.validate=false` on the command line or in `gradle.properties`, named as the Maven plugin's `-D` properties. A value in the build script wins.
- **Gradle: several schemas from one project**, by registering more `GenerateFlatBuffersSchema` tasks (see [Gradle plugin](#gradle-plugin)).
- **Fix**: an enum with `@FlatBufferEnumTypeField` values whose class targets a newer Java than the build's JVM was written with implicit values (`0, 1, …`), noted only in a validation comment. The build now fails and says why (see [Troubleshooting](#troubleshooting)).

No output changes for builds that worked before.

## What's new in 1.3.0

- **Gradle plugin** `io.github.mhradek.aurkitu`: the `generateFlatBuffersSchema` task, with the same options and output as the Maven plugin; cacheable and configuration-cache compatible (see [Gradle plugin](#gradle-plugin)).
- **`aurkitu-core`**: the schema generator as its own artifact, with no Maven dependency, shared by both plugins. The Maven plugin's parameters and output are unchanged.
- **Docs**: [Using the schema from other languages](#using-the-schema-from-other-languages), with the `flatc` options for C#, Swift, TypeScript, Go, Rust and more.

No output changes: schemas generated by 1.2.0 and 1.3.0 are the same.

## What's new in 1.2.0

- **`snakeCaseAcronyms`** (on by default): acronym-aware snake_case, so `userID` becomes `user_id` and `XMLParser` becomes `xml_parser` (in field names and `MapValueSet_*` table names).
- **`typedMapKeys`** (opt-in): map keys keep their Java type (`Map<Integer, V>` → `key:int`) and are marked `(key)` for lookups. Off by default, because it changes existing map tables.
- **Tests**: the option matrix covers both new options (14 options, 16384 combinations, run in parallel).
- **Build**: reproducible builds, and [SLSA](https://slsa.dev/) provenance on the GitHub release for the artifacts the release workflow rebuilds byte for byte (see [Publishing](#publishing)); SpotBugs with FindSecBugs; weekly mutation testing.

Output changes to expect when upgrading from 1.1:

- Field names with consecutive capitals change: `userID` is written `user_id` (was `user_i_d`), and `MapValueSet_*` tables of such fields are renamed the same way. Binary data is unaffected, since FlatBuffers stores fields by position or id, but code generated by `flatc` (e.g. Java `userID()` becomes `userId()`) and JSON field names change. Set `snakeCaseAcronyms` to `false` to keep the 1.1 names.
- Field names without consecutive capitals, and schemas written with `snakeCaseFields=false`, do not change.

## What's new in 1.1.1

A fix release. Aurkitu scanned only `public` classes, so a schema could lose parts without any report:

- the fields of a package-private superclass were left out;
- a `private` nested class was written as an empty table;
- a package-private `@FlatBufferTable` or `@FlatBufferEnum` was not declared, and fields using it were reported and left out.

Classes are now scanned whatever their visibility. Output changes to expect when upgrading from 1.1.0, only for models that use non-public classes:

- The missing fields, tables and enums are written. Fields inherited from a package-private superclass come after the table's own fields; if data written with 1.1.0 already exists, give the fields `fieldId`s first (see [Schema evolution](#schema-evolution)).
- A `private` nested class that 1.1.0 wrote as an empty table now has its fields. If it is a helper that is not part of the schema, mark it `@FlatBufferIgnore`.

## What's new in 1.1.0

- **Validation** of the FlatBuffers schema rules Aurkitu used to write without checking: `required`, defaults, struct-only and vector-only attributes, field ids, `key`, `hash`, `nested_flatbuffer`, duplicate field names, union `_type` clashes, empty structs and more (see [Validation](#validation)). Invalid parts are reported and left out, so the written schema compiles; the one exception is a table enum field whose enum has no `0` value and that needs a `defaultValue`, which is reported for you to fix. The rules are tested against `flatc` itself.
- **Warnings** for schemas `flatc` accepts but that are risky: inherited fields in tables without field ids ([Schema evolution](#schema-evolution)), and `force_align` on a field that is not a vector (`flatc` ignores it).
- **Documentation comments**: `@FlatBufferComment` writes `///` comments, which `flatc` copies into generated code, and now also works on enum constants, union members and RPC methods.
- **New features**: optional scalars (`optionalScalars`), `bit_flags` enums, union aliases (`@FlatBufferUnionMember`), RPC services (`@FlatBufferRpcService`), custom attributes (`attributes`), `original_order` and struct `force_align`.
- **Fixes**:
  - A map of maps (`Map<String, Map<String, V>>`) produced a table that referred to itself. The inner map now gets its own table, `MapValueSet_<Owner>_<field>_value`.
  - A `namespace` statement is written only where the namespace changes. Previously the default was restored after every type in another namespace.
  - A non-scalar struct field was left in unless `failOnValidationIssues` was set. It is now always left out.
  - Two fields written with the same name, or a field named like a union's hidden `_type` field, were reported but both written, which `flatc` rejects. The later one is now left out.
  - An optional enum field (`= null`) was reported as an invalid default.
  - A map value of an enum type without a `0` value (`Map<String, Status>`) got an issue that could not be fixed, because the generated field cannot be annotated. It now defaults to the enum's lowest value (`value:Status = ACTIVE;`).
  - An enum default that is not a value was kept even when the enum has a `0` value. It is now left out, so the field defaults to `0`.
- **Tests**: every generator option (including `schemaNamespace`) on and off in all 4096 combinations, on models that probe how the options affect one another; validation compared with `flatc` for every field type and attribute pair; an opt-in `pitest` profile for mutation testing.
- **Coveralls** receives both modules' coverage.

Output changes to expect when upgrading from 1.0.1:

- `//` comments become `///`.
- Fields, attributes and defaults that `flatc` rejects are left out and reported. These include references to undeclared types (without `includes`) and struct fields that are not scalars.
- `force_align` on a scalar is left out, with a warning.
- A map of maps gets a separate inner table.
- There are fewer `namespace` lines.
- `@version` hashes change wherever the schema text changes.

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

1. Update the dependency and plugin coordinates to `io.github.mhradek` and version `1.3.0`.
2. Change imports from `com.michaelhradek.aurkitu.*` to `io.github.mhradek.aurkitu.*`.
3. Rename plugin parameters as above.
4. Set `<snakeCaseFields>false</snakeCaseFields>` if clients depend on camelCase field names.
5. Regenerate the schemas and review the validation comments.

---

## Roadmap

| Priority | Item | Status |
|----------|------|--------|
| Medium | Published Javadoc site | Done: [javadoc.io](https://javadoc.io/doc/io.github.mhradek/aurkitu-annotations) serves the Javadoc jars from Maven Central |
| Low | Schema include resolution | Includes are written, not read |
| Low | Multiple schema files (one per namespace) | One file per Maven execution or Gradle task; several executions or tasks can split a project's types by package |
| Low | Gradle: run the generator on the project's Java toolchain | Models compiled for a newer Java than Gradle's JVM fail with an explanation if they use `@FlatBufferEnumTypeField` (see [Troubleshooting](#troubleshooting)) |
| Low | Typed map keys on by default | Opt-in since 1.2.0; a candidate for the default in 2.0 |
| Low | Remove deprecated `@FlatBufferOptions` | Planned for 2.0 |
| Low | Models in other languages (C#, TypeScript, Go, ...) | Clients in any language already use the generated schema through `flatc`; a port for models written in another language is considered on request |
| Low | Make `FieldType.targetClass` and `EnumType.targetClass` final | Public and assignable since 0.0.x, so a breaking change; planned for 2.0 |

## Security

See [SECURITY.md](SECURITY.md) for supported versions and how to report a vulnerability privately.

## License

MIT
