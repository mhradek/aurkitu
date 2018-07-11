package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.test.other.SampleClassNamespaceMap;
import java.util.List;

/**
 * @author m.hradek
 */
@FlatBufferTable
public class SampleClassReferenced extends SampleClassAbstract {

    protected long id;
    protected List<SampleClassTable> baggage;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    protected List<SampleClassNamespaceMap> samples;

    public enum SampleClassTableInnerEnumInt {
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
