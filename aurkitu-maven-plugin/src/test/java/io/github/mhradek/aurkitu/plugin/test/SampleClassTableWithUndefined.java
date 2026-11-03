package io.github.mhradek.aurkitu.plugin.test;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/**
 * @author m.hradek
 */
@FlatBufferTable
public class SampleClassTableWithUndefined {

    protected long id;
    protected String message;
    protected SimpleUndefinedClass awesomeUndefinedClass;
}
