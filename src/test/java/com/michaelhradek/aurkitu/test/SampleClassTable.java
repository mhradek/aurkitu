package com.michaelhradek.aurkitu.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferComment;
import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.core.output.FieldType;
import com.michaelhradek.aurkitu.test.SampleClassReferenced.SampleClassTableInnerEnumInt;
import java.util.List;
import java.util.Map;

/**
 * @author m.hradek
 */
@FlatBufferComment(comment = "This is a type level comment")
@FlatBufferTable(rootType = true)
public class SampleClassTable {

    protected long id;
    protected String name;
    @FlatBufferComment(comment = "This is a field level comment")
    protected short level;
    protected int currency;
    protected long createTime;
    protected List<String> tokens;
    protected boolean deleted = false;
    protected byte energy;
    protected Double weight;
    protected int[] options;
    protected SimpleUndefinedClass[] anomalousSamples;
    public Map<String, Object> dataMap;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    protected SampleClassTableInnerEnumInt[] definedInnerEnumArray;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SHORT_SWORD")
    protected SampleClassTableInnerEnumInt innerEnum;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    SampleClassReferenced fullnameClass;

    @FlatBufferComment(comment = "This is a comment which won't appear")
    @FlatBufferIgnore
    protected String ignore;
}
