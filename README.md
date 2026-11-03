# Aurkitu 1.0.0

[![Build](https://github.com/mhradek/aurkitu/actions/workflows/main.yml/badge.svg)](https://github.com/mhradek/aurkitu/actions)

**Aurkitu** (Basque for “to find”) generates FlatBuffers IDL (`.fbs`) schemas from annotated Java models. This keeps your server-side models as the single source of truth.

This is the **modernised 1.0.0** release:
- Targets **Java 17**
- Uses **ClassGraph** for reliable classpath scanning (replaces the old custom Reflections fork)
- Simplified architecture with clearer separation of concerns
- Updated dependencies and Maven plugin toolchain
- Ready for current FlatBuffers (25.x series)

## Quick Start

Add the annotations:

```xml
<dependency>
    <groupId>com.michaelhradek</groupId>
    <artifactId>aurkitu-annotations</artifactId>
    <version>1.0.0</version>
</dependency>
```

Configure the plugin:

```xml
<plugin>
    <groupId>com.michaelhradek</groupId>
    <artifactId>aurkitu-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <schemaName>user</schemaName>
        <schemaNamespace>com.example.flatbuffers</schemaNamespace>
        <!-- Optional: limit scanning for speed -->
        <specifiedPackages>
            <specifiedPackage>com.example.models</specifiedPackage>
        </specifiedPackages>
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

Annotate your models:

```java
@FlatBufferTable(rootType = true)
public class User {
    private long id;
    private String name;
    // ...
}
```

Run `mvn process-classes` (or a later phase) to generate `target/aurkitu/schemas/user.fbs`.

## Status

1.0.0 is a clean modern foundation. Full field-level processing, advanced type support (maps, lists, defaults, etc.), and complete validation against current `flatc` are being expanded on top of the ClassGraph-based scanner.

See the original 0.0.x series for the historical proof-of-concept behaviour.

## Building

Requires JDK 17+.

```bash
mvn clean verify
```

## License

MIT
