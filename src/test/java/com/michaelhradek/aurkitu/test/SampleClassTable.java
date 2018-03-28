/**
 *
 */
package com.michaelhradek.aurkitu.test;

import java.util.List;

import com.michaelhradek.aurkitu.annotations.FlatBufferIgnore;
import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

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

    @FlatBufferIgnore
    protected String ignore;
}
