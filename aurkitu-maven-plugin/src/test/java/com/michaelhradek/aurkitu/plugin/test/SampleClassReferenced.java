package com.michaelhradek.aurkitu.plugin.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.test.other.SampleClassNamespaceMap;

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

    public static class InnerClassStatic {
        public boolean virulant;
    }

    public class InnerClass {
        public boolean processed;
        public SampleClassTableInnerEnumInt weaponType;
    }
}
