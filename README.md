# aurkitu 

[![Build Status](https://travis-ci.org/mhradek/aurkitu.svg?branch=master)](https://travis-ci.org/mhradek/aurkitu)
[![Code Climate](https://codeclimate.com/github/mhradek/aurkitu.svg)](https://codeclimate.com/github/mhradek/aurkitu)


Aurkitu is the Basque word for for the English phrase "to find". This project is intended to assist in the automatic generation of FlatBuffer IDL/schemas from Java source. 

* FlatBuffers: https://google.github.io/flatbuffers/
* Building schemas with Maven: https://github.com/davidmoten/flatbuffers

## please note 
This is a very early proof-of-concept currently being developed in spare time.

## roadmap
- [ ] handle core types
- [ ] handle options, deprecation, and default values
- [ ] validate dependencies
- [ ] implement as plugin and test against a sample project
- [ ] test with flatc
- [ ] release to maven

## integration
To be determined.  The goal is to use as a Maven-styled plugin.

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
// @version: 00000000d84b64aa7fd7fa5a8b4603a87ff1849e

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
