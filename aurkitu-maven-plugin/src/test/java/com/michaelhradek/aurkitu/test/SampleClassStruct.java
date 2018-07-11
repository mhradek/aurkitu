/**
 *
 */
package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;

/**
 * @author m.hradek
 */
@FlatBufferTable(TableStructureType.STRUCT)
public class SampleClassStruct {

    protected float x;
    protected float y;
    protected float z;
}
