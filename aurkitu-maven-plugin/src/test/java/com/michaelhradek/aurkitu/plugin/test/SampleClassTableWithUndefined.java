package com.michaelhradek.aurkitu.plugin.test;

import com.michaelhradek.aurkitu.annotations.FlatBufferTable;

/**
 * @author m.hradek
 */
@FlatBufferTable
public class SampleClassTableWithUndefined {

    protected long id;
    protected String message;
    protected SimpleUndefinedClass awesomeUndefinedClass;
}
