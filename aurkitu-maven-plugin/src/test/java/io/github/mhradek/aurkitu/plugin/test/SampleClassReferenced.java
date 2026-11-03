package io.github.mhradek.aurkitu.plugin.test;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;
import io.github.mhradek.aurkitu.plugin.test.other.SampleClassNamespaceMap;

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
