/**
 * 
 */
package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@FlatBufferTable(TableStructureType.STRUCT)
public class SampleClassStruct {

  protected float x;
  protected float y;
  protected float z;
}
