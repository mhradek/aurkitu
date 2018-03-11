# aurkitu 

[![Build Status](https://travis-ci.org/mhradek/aurkitu.svg?branch=master)](https://travis-ci.org/mhradek/aurkitu)
[![codecov](https://codecov.io/gh/mhradek/aurkitu/branch/master/graph/badge.svg)](https://codecov.io/gh/mhradek/aurkitu)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.michaelhradek/aurkitu-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.michaelhradek/aurkitu-maven-plugin)


Aurkitu is the Basque word for for the English phrase "to find". This project is intended to assist in the automatic generation of FlatBuffer IDL/schemas from Java source. But isn't this backwards? Shouldn't the schema generate the models for both client and service? Unfortunately, when using models which are stored in cloud based storage mechanisms (e.g. AWS Dynamo DB) requires that table definitions, keys, and other attirubtes to be declared within the model. Hence with this project the aim is to allow the server implementation to remain as the master version of these models making the schema, flatbuffers, and client code as auxillery. 

* FlatBuffers: https://google.github.io/flatbuffers/
* Building schemas with Maven: https://github.com/davidmoten/flatbuffers

## please note 
This is a very early proof-of-concept currently being developed in spare time.

## roadmap
- [x] handle core types
- [ ] handle options, deprecation, and default values
- [ ] validate dependencies (optional)
- [ ] implement as plugin and test against a sample project
- [ ] test with flatc
- [x] release to maven

## integration
Add the following to your dependencies within your `pom.xml`:
```
<dependency>
    <groupId>com.michaelhradek</groupId>
    <artifactId>aurkitu-maven-plugin</artifactId>
    <version>0.0.2.3</version>
</dependency>
```
Followed by the following to the `plugins` of your `build` specifications:
```
<plugin>
    <groupId>com.michaelhradek</groupId>
    <artifactId>aurkitu-maven-plugin</artifactId>
    <configuration>
        <schemaName>user</schemaName>
        <searchPath>com.company.package.subpackage.*</searchPath>
        <schemaNamespace>com.company.package.subpackage.flatbuffers</schemaNamespace>
        <schemaIncludes>
             <include>"../../../../target/maven-shared-archive-resources/flatbuffers/other.fbs"</include>
        </schemaIncludes>
        <validateSchema>true</validateSchema>
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
## usage
Through the use of annotations:
```
@FlatBufferTable
public class SampleClassReferenced {
...
```
Specify a root type:
```
@FlatBufferTable(rootType = true)
public class SampleClassTable {
...
```
Specify different structure declarations:
```
@FlatBufferTable(TableStructureType.STRUCT)
public class SampleClassStruct {
...
}

@FlatBufferEnum(enumType = FieldType.BYTE)
public enum SampleEnumByte {
...
```

Ignore fields:
```
@FlatBufferIgnore
protected String ignore;
```

## sample output
This sample is generated by the JUnit tests within the project into a `test.fbs` file.

```
// Aurkitu automatically generated IDL FlatBuffer Schema
// @version: b71d0076

include AnotherFile.fbs;

attribute "Priority";
attribute "ConsiderThis";

namespace: com.michaelhradek.aurkitu.flatbuffers;

enum SampleClassTableInnerEnumInt { DAGGER, SHORT_SWORD, SWORD, GREAT_SWORD }

enum SampleEnumByte : byte { EnvironmentAlpha = 1, EnvironmentBeta = 2, EnvironmentGamma = 3 }

enum SampleEnumNull { PlatformAlpha, PlatformBeta, PlatformGamma }

table SampleClassReferenced {
  id:long;
  baggage:[SampleClassTable];
  abstractField:string;
}

table InnerClassStatic {
  virulant:bool;
}

table InnerClass {
  processed:bool;
  weaponType:SampleClassTableInnerEnumInt;
}

table SampleClassTable {
  id:long;
  name:string;
  level:short;
  currency:int;
  createTime:long;
  tokens:[string];
  deleted:bool;
  energy:byte;
}

struct SampleClassStruct {
  x:float;
  y:float;
  z:float;
}

root_type: SampleClassTable;
```      
