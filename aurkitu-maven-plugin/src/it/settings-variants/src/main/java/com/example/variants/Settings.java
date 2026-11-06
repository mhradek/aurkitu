package com.example.variants;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.util.UUID;

@FlatBufferTable(rootType = true)
public class Settings {
    /** Emitted because ignoreStaticMembers=false. */
    static int schemaRevision;

    /** Stays camelCase because snakeCaseFields=false. */
    private String displayName;

    /** Still a string with mapCommonTypes=false... */
    private String plainText;

    /** ...but UUID is not mapped, so it is reported and skipped. */
    private UUID externalId;

    /** char has no FlatBuffers equivalent: reported and skipped. */
    private char initial;

    /** Only some fields have ids: ids are omitted and the issue reported. */
    @FlatBufferFieldOptions(fieldType = FieldType.INT, fieldId = 0)
    private int withId;

    private int withoutId;
}
