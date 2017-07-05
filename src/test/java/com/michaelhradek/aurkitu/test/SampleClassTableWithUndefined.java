/**
 * 
 */
package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

/**
 * @author m.hradek
 * @date May 22, 2017
 * 
 */
@FlatBufferTable
public class SampleClassTableWithUndefined {

  protected long id;
  protected String message;
  protected SimpleUndefinedClass awesomeUndefinedClass;
}
