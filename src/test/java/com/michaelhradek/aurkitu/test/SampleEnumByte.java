/**
 * 
 */
package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferEnum;
import com.michaelhradek.aurkitu.core.output.FieldType;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@FlatBufferEnum(enumType = FieldType.BYTE)
public enum SampleEnumByte {
  EnvironmentAlpha((byte) 1), EnvironmentBeta((byte) 2), EnvironmentGamma((byte) 3);

  byte id;

  SampleEnumByte(byte id) {
    this.id = id;
  }
}
