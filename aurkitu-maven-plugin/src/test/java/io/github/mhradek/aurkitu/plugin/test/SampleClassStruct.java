package io.github.mhradek.aurkitu.plugin.test;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable.TableStructureType;

/**
 * @author m.hradek
 */
@FlatBufferTable(TableStructureType.STRUCT)
public class SampleClassStruct {

    protected float x;
    protected float y;
    protected float z;
}
