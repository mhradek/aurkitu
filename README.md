# aurkitu 

[![Build Status](https://travis-ci.org/mhradek/aurkitu.svg?branch=master)](https://travis-ci.org/mhradek/aurkitu)
[![Code Climate](https://codeclimate.com/github/mhradek/aurkitu.svg)](https://codeclimate.com/github/mhradek/aurkitu)


Aurkitu is the Basque word for for the English phrase "to find". This project is intended to assist in the automatic generation of FlatBuffer IDL/schemas from Java source.

* FlatBuffers: https://google.github.io/flatbuffers/
* Building schemas with Maven: https://github.com/davidmoten/flatbuffers

## please note 
This is a very early proof-of-concept currently being developed in spare time.

## sample output
```
// Aurkitu automatically generated IDL FlatBuffer Schema

namespace: null;

enum SampleEnumNull { PlatformAlpha, PlatformBeta, PlatformGamma }

enum SampleEnumByte : byte { EnvironmentAlpha, EnvironmentBeta, EnvironmentGamma }

table SampleClassReferenced {
  id:long;
  baggage:[SampleClassTable];
}

struct SampleClassStruct {
  x:float;
  y:float;
  z:float;
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

root_type: SampleClassTable;
```
