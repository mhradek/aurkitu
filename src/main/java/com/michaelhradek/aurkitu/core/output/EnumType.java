/**
 * 
 */
package com.michaelhradek.aurkitu.core.output;

/**
 * @author m.hradek
 * @date May 23, 2017
 * 
 */
public enum EnumType {
  ENUM, UNION;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
