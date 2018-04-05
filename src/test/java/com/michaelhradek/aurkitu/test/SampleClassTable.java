package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.FieldType;
import com.michaelhradek.aurkitu.test.SampleClassReferenced.SampleClassTableInnerEnumInt;
import java.util.List;

/**
 * @author m.hradek
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
    protected Double weight;
    protected int[] options;
    protected SimpleUndefinedClass[] anomalousSamples;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    protected SampleClassTableInnerEnumInt[] definedInnerEnumArray;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SHORT_SWORD")
    protected SampleClassTableInnerEnumInt innerEnum;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    SampleClassReferenced fullnameClass;

    @FlatBufferIgnore
    protected String ignore;
}
