package com.michaelhradek.aurkitu.plugin;

/**
 * @author m.hradek
 */
public final class Config {

    /**
     * Only use on your local dev box.
     */
    public static final boolean DEBUG = false;

    /**
     *
     */
    public static final String FILE_EXTENSION = "fbs";

    /**
     * At the top of all generated schema files
     */
    public static final String SCHEMA_INTRO_COMMENT =
            "// Aurkitu automatically generated IDL FlatBuffer Schema";
    /**
     * The placeholder for the optional version line specified at
     * {@link Config#SCHEMA_VERSION_COMMENT}
     */
    public static final String SCHEMA_VERSION_PLACEHOLDER = "AURKITU-SCHEMA-VERSION-ghjtyu567FHGFD";

    /**
     * Optional which includes a {@link String#hashCode()} of the contents of the schema file.
     */
    public static final String SCHEMA_VERSION_COMMENT =
            "// @version: " + Config.SCHEMA_VERSION_PLACEHOLDER;

    /**
     * In a schema namespace, the default is &lt;groupId&gt;.&lt;SCHEMA_NAMESPACE_IDENTIFIER_DEFAULT&gt;.&lt;aetifactId&gt;
     */
    public static final String SCHEMA_NAMESPACE_IDENTIFIER_DEFAULT = "flatbuffers";
}
