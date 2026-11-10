# Aurkitu sample

`game/` is a small, complete project: annotated Java models of a game's player, items and events, and the
FlatBuffers schema Aurkitu writes from them. The same sources build with **Maven** (`pom.xml`) or **Gradle**
(`build.gradle.kts`), and both write the same schema, checked in as [`game/expected/game.fbs`](game/expected/game.fbs).

For every option and annotation, see the [main README](../README.md). The Maven plugin's end-to-end test
project [`kitchen-sink`](../aurkitu-maven-plugin/src/it/kitchen-sink) uses every feature, edge cases included.

## Run it

Requires Java 17 or later, and Maven or Gradle (8.14 or later):

```bash
cd samples/game && mvn package
```

```bash
cd samples/game && gradle build
```

Maven writes `target/aurkitu/schemas/game.fbs`, Gradle `build/aurkitu/schemas/game.fbs`. The Aurkitu version is
`aurkituVersion` in `pom.xml` and `gradle.properties`.

Then generate code for the languages you need with [`flatc`](https://github.com/google/flatbuffers/releases):

```bash
flatc --java --ts --csharp -o generated target/aurkitu/schemas/game.fbs
```

## From models to schema

The configuration (the same in both builds): schema `game`, namespace `com.example.game.fb`, only the
`com.example.game` package, file identifier `GAME`, and `failOnValidationIssues`, so a schema problem fails the
build.

| Java ([`game/src/main/java/com/example/game`](game/src/main/java/com/example/game)) | Schema |
|------|--------|
| `@FlatBufferTable(rootType = true) class Player` | `table Player { ... }` and `root_type Player;` |
| `private long id;` | `id:long;` |
| `@FlatBufferFieldOptions(fieldType = STRING, required = true) private String displayName;` | `display_name:string (required);` (snake_case names) |
| `@FlatBufferFieldOptions(fieldType = INT, defaultValue = "1") private int level;` | `level:int = 1;` |
| `@FlatBufferComment(comment = "Starts at 1.")` | `/// Starts at 1.`, which `flatc` copies into generated code |
| `private Vec3 position;`, `Vec3` a `STRUCT` of three floats | `position:Vec3;` and `struct Vec3 { x:float; ... }` |
| `private List<Item> inventory;` | `inventory:[Item];` |
| `private Map<String, Integer> stats;` | `stats:[MapValueSet_Player_stats];` and a `MapValueSet_Player_stats` table, since FlatBuffers has no map type |
| `private Event lastEvent;`, `Event` a `UNION` of `LevelUp` and `ItemFound` | `last_event:Event;` and `union Event { LevelUp, ItemFound, }` |
| `@FlatBufferIgnore private String sessionToken;` | Nothing: left out |
| `enum Status { OFFLINE, ONLINE, IN_MATCH }` | `enum Status : byte { OFFLINE, ONLINE, IN_MATCH, }`, values from declaration order |
| `enum Rarity`, values `1`, `5`, `10` in a `@FlatBufferEnumTypeField` | `enum Rarity : ubyte { COMMON = 1, RARE = 5, LEGENDARY = 10, }` |
| `Item.rarity` with `defaultValue = "COMMON"` | `rarity:Rarity = COMMON;`: `Rarity` has no `0`, so its fields need a default |

## Checked in CI

`samples/verify.sh --this-checkout` builds the sample with both build tools against the current code, checks that
both schemas match `game/expected/game.fbs` (apart from the version in the first line), and compiles it with
`flatc`. CI runs it on every change, so this sample always shows what Aurkitu really writes. If a change to
Aurkitu changes the schema on purpose, update `game/expected/game.fbs` in the same commit.
