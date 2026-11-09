package io.github.mhradek.aurkitu.plugin.core.acronyms;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Field names with acronyms; see {@code AcronymDefaultTest}. */
@FlatBufferTable(rootType = true)
public class Profile {
    long userID;
    String profileURL;
    String xmlParser;
}
