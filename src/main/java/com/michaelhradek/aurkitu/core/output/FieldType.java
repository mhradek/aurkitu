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
  BOOL, BYTE, UBYTE, SHORT, USHORT, INT, UINT, FLOAT, LONG, ULONG, DOUBLE, STRING, IDENT, ARRAY;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
