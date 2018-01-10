/**
 *
 */
package com.michaelhradek.aurkitu.test;

import java.util.List;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

/**
 * @author m.hradek
 */
@FlatBufferTable
public class SampleClassReferenced extends SampleClassAbstract {

    protected long id;
    protected List<SampleClassTable> baggage;

    enum SampleClassTableInnerEnumInt {
        DAGGER, SHORT_SWORD, SWORD, GREAT_SWORD
    }

    public class InnerClass {
        public boolean processed;
        public SampleClassTableInnerEnumInt weaponType;
    }

    public static class InnerClassStatic {
        public boolean virulant;
    }
}
