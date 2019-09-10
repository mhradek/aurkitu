package com.michaelhradek.aurkitu.plugin.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferComment;
import com.michaelhradek.aurkitu.annotations.FlatBufferFieldOptions;
import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;
import com.michaelhradek.aurkitu.annotations.types.FieldType;
import com.michaelhradek.aurkitu.plugin.test.SampleClassReferenced.SampleClassTableInnerEnumInt;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author m.hradek
 */
@FlatBufferComment(comment = "This is a type level comment")
@FlatBufferTable(rootType = true)
public class SampleClassTable {

    public static String IGNORED_STATIC_FIELD = "ignoredStaticField";

    public Map<String, Object> dataMap;
    public Set<URL> regionLocations;
    protected Long id;
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
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    protected SampleClassTableInnerEnumInt[] definedInnerEnumArray;

    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "SHORT_SWORD")
    protected SampleClassTableInnerEnumInt innerEnum;
    @FlatBufferComment(comment = "This is a comment which won't appear")
    @FlatBufferIgnore
    protected String ignore;
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, useFullName = true)
    SampleClassReferenced fullnameClass;

    public List<SampleEnumNull> enumList;
    public Map<SampleEnumNull, String> enumStringMap;
    public Map<SampleEnumNull, SampleClassTableInnerEnumInt> enumInnerEnumMap;

    protected Integer integerField;
    protected Short shortField;
    protected Boolean booleanField;
    protected Byte byteField;
    protected Float floatField;
    protected double doubleField;

    protected SampleClassReferenced.InnerClass innerClassField;
}
