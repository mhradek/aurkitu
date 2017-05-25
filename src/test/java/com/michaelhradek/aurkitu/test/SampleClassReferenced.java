/**
 * 
 */
package com.michaelhradek.aurkitu.test;

import java.util.List;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@FlatBufferTable
public class SampleClassReferenced {

  protected long id;
  protected List<SampleClassTable> baggage;

  public class InnerClass {
    public boolean processed;
  }

  public static class InnerClassStatic {
    public boolean virulant;
  }
}
