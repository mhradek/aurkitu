# aurkitu 

[![Latest release](https://img.shields.io/github/release/mhradek/aurkitu.svg)](https://github.com/mhradek/aurkitu/releases/latest)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/706cead2b38643d2be38efbae95f40ed)](https://app.codacy.com/app/mhradek/aurkitu?utm_source=github.com&utm_medium=referral&utm_content=mhradek/aurkitu&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/mhradek/aurkitu.svg?branch=master)](https://travis-ci.org/mhradek/aurkitu)
[![codecov](https://codecov.io/gh/mhradek/aurkitu/branch/master/graph/badge.svg)](https://codecov.io/gh/mhradek/aurkitu)
[![Coverage Status](https://coveralls.io/repos/github/mhradek/aurkitu/badge.svg?branch=master)](https://coveralls.io/github/mhradek/aurkitu?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.michaelhradek/aurkitu-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.michaelhradek/aurkitu-maven-plugin)

Aurkitu is the Basque word for for the English phrase "to find". This project is intended to assist in the automatic generation of FlatBuffer IDL/schemas from Java source. But isn't this backwards? Shouldn't the schema generate the models for both client and service? Unfortunately, when using models which are stored in cloud based storage mechanisms (e.g. AWS Dynamo DB, annotations, etc.) requires that table definitions, keys, and other attributes to be declared within the model. Hence with this project the aim is to allow the server implementation to remain as the master version of these models making the schema, flatbuffers, and client code as auxillery. This strategy enables engineers to make full use of the toolchains and avoid some of the pitfalls of manually updated files. 

*   FlatBuffers: <https://google.github.io/flatbuffers/>
*   Building schemas with Maven: <https://github.com/davidmoten/flatbuffers>

Aurkitu currently supports Flatbuffers version 1.3.

## please note 
This is a very early proof-of-concept currently being developed in spare time.

## roadmap
*   [ ] handle options, deprecation, and default values
*   [ ] improve validation around various types (e.g. `char`, `Character`, `String`, `Boolean`, `bool`, etc.)
*   [ ] improve testing (i.e. fix plugin test harness, better integration test coverage, etc.) and move these from `build-test.sh` into a Maven target
*   [ ] add support for common Java types such as `java.net.URL`, `java.util.Date`, etc. This should be an optional feature enabled by flag.
*   [ ] add deployment to Maven and remove `package.sh`
*   [x] run schema generated by plugin tests against `flatc`
*   [x] handle ordering of table definitions (alphabetical)
*   [ ] update with current (1.8) feature support (i.e. gRPC, Field, String constant, etc.)

## peculiarities
While flatbuffers support unsigned primatives (e.g. ubyte, ushort, etc.), Java does not technically support them (though you can use the wrapper types \[e.g. `java.lang.Long`, etc.]) to [simulate this behavior](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html). Eventually we could map to the wrapper types when building the schema files. At this time all primatives and their corresponding wrappers are mapped as primatives.

Aurkitu supports `java.util.Map` by creating an autogenerated key-value type which is then used via list in the defining type. It is a work around and the parsers using generated flatbuffers will need to translate accordingly. The name format is `MapValueSet_<type>_<field>`. The server and client will need to implement custom code to translate back and forth between `Object{K, V}[]` and `Map<K, V>`. The supported types for keys should be any simple type (i.e. Enum, String, Double, etc.). The values can be simple types and Lists of simple types. 

Using `specifiedDependencies` is highly recommended to keep package scanning time to a minimum. Otherwise, all classes/depndencies will be scanned for annotations.

## integration
### Annotations
Add the following to your dependencies within your `pom.xml`:
```XML
<dependency>
    <groupId>com.michaelhradek</groupId>
    <artifactId>aurkitu-annotations</artifactId>
    <version>0.0.7.3</version>
</dependency>
```
### Maven Plugin
Followed by the following to the `plugins` of your `build` specifications within your `pom.xml`:
```XML
<plugin>
    <groupId>com.michaelhradek</groupId>
    <artifactId>aurkitu-maven-plugin</artifactId>
    <version>0.0.7.3</version>
    <configuration>
        <schemaName>user</schemaName>
        <schemaNamespace>com.company.package.subpackage.flatbuffers</schemaNamespace>
        <specifiedDependencies>
             <depdendency>com.company.department:artifact-id</depdendency>
             <depdendency>com.other.subteam</depdendency>
        </specifiedDependencies>
        <consolidatedSchemas>true</consolidatedSchemas>
        <schemaIncludes>
             <include>"../../../../target/maven-shared-archive-resources/flatbuffers/other.fbs"</include>
        </schemaIncludes>
        <validateSchema>true</validateSchema>
        <namespaceOverrideMap>
	          <com.company.package.search>com.company.package.replace</com.company.package.search>
        </namespaceOverrideMap>   
        <useSchemaCaching>false</useSchemaCaching>     
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
### option definitions
#### required
*   __schemaName__: sets the name of the generated schema which is then used for the output filename (e.g. `<schemaName>.<flatcExtention>` or, for example, `myschema.fbs`)
#### optional
*   __schemaNamespace__: sets the namespace of schema. All objects in this schema will have this namespace (default: `generated.flatbuffers`). The options are `group_id:identifier:artifact_id`. To omit, follow pattern `::artifact_id`.
*   __flatcExtention__: sets the output file extension (default `fbs`)
*   __schemaFileIdentifier__: flatbuffer file identifier (e.g. `file_identifier "MYFI";`)
*   __outputDir__: where the generated schema will be written to (default: `${project.build.directory}/aurkitu/schemas`)
*   __validateSchema__: if true, validate the schema and append the report to the end of the schema file as a list of comments (default: `true`)
*   __generateVersion__: if true, generate a version for the schema which excludes the validation text and then add it as a comment to the top of the schema file (default: `false`)
*   __useSchemaCaching__: if true, skip schema generation. The file that is located in the output directory will be used. Designed for local development build speed improvement (default: `false`)
*   __namespaceOverrideMap__: allows for schema namespaces to be overriden. This is handy when using includes and schemas from other projects (e.g `<com.company.package.search>com.company.package.replace</com.company.package.search>`)
*   __schemaIncludes__: allows for configuration of schema includes (e.g. `<include>"../../../../target/maven-shared-archive-resources/flatbuffers/other.fbs"</include>`)
*   __specifiedDependencies__: allows for configuration of targeted dependency searching for specific dependencies for annotations. If this is specified, a artifact resolution will be kept to a minimum greatly increasing build speed. You can specify any number of packages. If you specify the `groupId` only then the entirety of the group will be included in artifact resolution (e.g. `<dependency>com.company.group</dependency>`) . To specify a specific artifact use `groupId:artifactId` (e.g. `<dependency>com.company.group:artifact</dependency>`)
*   __consolidatedSchemas__: if true, create one schema. If false, create one schema for the project and then one schema _per dependecy_. (default: `true`) This is useful in situations where the dependencies are used across projects where namespaces are useful.
*   __ignoreStaticMembers__: if true, ignore class member variables which are static. (default: `true`)
#### planned
*   __buildDependencySchemas__: if true, build, validate, and write all the dependency schema. If false, it will still need to build them to verify the target schema but won't validate and write them out. (default: `true`)
  

## usage
Through the use of annotations:
```java
@FlatBufferTable
public class SampleClassReferenced {

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    SampleClassReferenced fullnameClass;
    
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SHORT_SWORD")
    protected SampleClassTableInnerEnumInt innerEnum;
    // ...
}
``` 
Specify a root type:
```java
@FlatBufferTable(rootType = true)
public class SampleClassTable {
    // ...
}
```
Specify different structure declarations:
```java
@FlatBufferTable(TableStructureType.STRUCT)
public class SampleClassStruct {
    // ...
}

@FlatBufferEnum(enumType = FieldType.BYTE)
public enum SampleEnumByte {

    // If you specify a enumType you will need to specify which field represents the type (as there can be several fields within an `ENUM`)
    @FlatBufferEnumTypeField
    byte id;
    
    String description;
    
    SampleEnumByte(byte id, String description) {
    // ...
    }
}
```

Ignore fields:
```java
@FlatBufferIgnore
protected String ignore;
```

## sample output
This sample is generated by the JUnit tests within the project into a `test.fbs` file.

```IDL
// Aurkitu automatically generated IDL FlatBuffer Schema
// @version: d9bf953d

enum Matrix : int { SMALL = 10000, MEDIUM = 20000, LARGE = 30000 }

enum Option { FirstOption, SecondOption, ThirdOption }

enum SampleClassTableInnerEnumInt { DAGGER, SHORT_SWORD, SWORD, GREAT_SWORD }

enum SampleEnumByte : byte { EnvironmentAlpha = 1, EnvironmentBeta = 2, EnvironmentGamma = 3 }

enum SampleEnumNull : byte { PlatformAlpha, PlatformBeta, PlatformGamma }

enum SplineEstimate : long { SMALL = 10000, MEDIUM = 20000, LARGE = 30000 }

enum TestEnumCommentEmpty : byte { }

// This is a enum comment
enum VectorSize : short { SMALL = 10000, MEDIUM = 20000, LARGE = 30000 }

table InnerClass {
  processed:bool;
  weaponType:SampleClassTableInnerEnumInt;
}

table InnerClassStatic {
  virulant:bool;
}

// Auto-generated type for use with Map<?, ?>
table MapValueSet_SampleClassTable_dataMap {
  key:string;
  value:string;
}

// Auto-generated type for use with Map<?, ?>
table MapValueSet_SampleClassTable_enumInnerEnumMap {
  key:SampleEnumNull;
  value:SampleClassTableInnerEnumInt;
}

// Auto-generated type for use with Map<?, ?>
table MapValueSet_SampleClassTable_enumStringMap {
  key:SampleEnumNull;
  value:string;
}

table SampleAnonymousEnum {
  option:Option;
  size:VectorSize;
  estimate:SplineEstimate;
  matrix:Matrix = SMALL;
}

table SampleClassNamespaceMap {
  id:string;
}

table SampleClassReferenced {
  id:long;
  baggage:[SampleClassTable];
  samples:[com.michaelhradek.aurkitu.plugin.test.other.SampleClassNamespaceMap];
  abstractField:string;
}

struct SampleClassStruct {
  x:float;
  y:float;
  z:float;
}

// This is a type level comment
table SampleClassTable {
  IGNORED_STATIC_FIELD:string;
  dataMap:[MapValueSet_SampleClassTable_dataMap];
  regionLocations:[URL];
  id:long;
  name:string;
  level:short;	// This is a field level comment
  currency:int;
  createTime:long;
  tokens:[string];
  deleted:bool;
  energy:byte;
  weight:double;
  options:[int];
  anomalousSamples:[SimpleUndefinedClass];
  definedInnerEnumArray:[com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced$SampleClassTableInnerEnumInt];
  innerEnum:SampleClassTableInnerEnumInt = SHORT_SWORD;
  fullnameClass:com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced;
  enumList:[SampleEnumNull];
  enumStringMap:[MapValueSet_SampleClassTable_enumStringMap];
  enumInnerEnumMap:[MapValueSet_SampleClassTable_enumInnerEnumMap];
  integerField:int;
  shortField:short;
  booleanField:bool;
  byteField:byte;
  floatField:float;
  doubleField:double;
  innerClassField:InnerClass;
}

table SampleClassTableWithUndefined {
  id:long;
  message:string;
  awesomeUndefinedClass:SimpleUndefinedClass;
}

root_type SampleClassTable;
```

## sample validation output
If `validateSchema` is set a comment similar to the following example block will be appended to the schema. The comments will be added to the end of the generated schema file which should assist in the resolution of issues which may likely cause `flatc` to fail.
```IDL
// Schema failed validation (i.e. flatc will likely fail): 
// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTable, Name: anomalousSamples
// Issue : INVALID_PATH, Location: SampleClassTable, Name: definedInnerEnumArray, Comment: Array type name contains '$'; using '@FlatBufferOptions(useFullName = true)' on inner not recommended: SampleClassReferenced$SampleClassTableInnerEnumInt
// Issue : TYPE_DEFINITION_NOT_DEFINED, Location: SampleClassTableWithUndefined, Name: awesomeUndefinedClass
// Issue : MISCONFIGURED_DEFINITION, Location: Option, Name: null
// Issue : MISCONFIGURED_DEFINITION, Location: SampleClassTableInnerEnumInt, Name: null
```      

## compile to flatbuffers
The `flatc` executable can be downloaded or compiled from the Flatbuffer project site. For example:
```commandline
echo "Compiling schemas to java"
target/bin/flatc --java --gen-mutable -o target/aurkitu/output/java target/aurkitu/schemas/*.fbs

echo "Compiling schemas to cpp"
target/bin/flatc --cpp -o target/aurkitu/output/cpp target/aurkitu/schemas/*.fbs
```

All this can be automated via Maven. Examples can be found in the `aurkitu-test-service/pom-test.xml`.
