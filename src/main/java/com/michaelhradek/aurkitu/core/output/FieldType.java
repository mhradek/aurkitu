/**
 * 
 */
package com.michaelhradek.aurkitu.core.output;

/**
 * @author m.hradek
 * @date May 23, 2017
 * 
 */
public enum FieldType {
  BOOL(boolean.class), BYTE(byte.class), UBYTE(byte.class), SHORT(short.class), USHORT(
      short.class), INT(int.class), UINT(int.class), FLOAT(float.class), LONG(long.class), ULONG(
          long.class), DOUBLE(double.class), STRING(String.class), IDENT(null), ARRAY(null);

  public Class<?> targetClass;

  FieldType(Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
