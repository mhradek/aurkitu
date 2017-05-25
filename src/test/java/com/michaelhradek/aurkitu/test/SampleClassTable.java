/**
 * 
 */
package com.michaelhradek.aurkitu.test;

import java.util.List;

import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@FlatBufferTable(rootType = true)
public class SampleClassTable {

  protected long id;
  protected String name;
  protected short level;
  protected int currency;
  protected long createTime;
  protected List<String> tokens;
  protected boolean deleted = false;
  protected byte energy;

  @FlatBufferIgnore
  protected String ignore;
}
