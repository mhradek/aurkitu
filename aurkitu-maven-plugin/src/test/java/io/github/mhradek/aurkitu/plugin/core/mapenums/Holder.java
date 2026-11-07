package io.github.mhradek.aurkitu.plugin.core.mapenums;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.util.Map;

/** Map values of enum types, and a table field of an enum without 0 that has its own default. */
@FlatBufferTable(rootType = true)
public class Holder {
    private Map<String, Code> codes;
    private Map<String, Color> colors;
    private Map<String, Perm> perms;
    @FlatBufferFieldOptions(fieldType = FieldType.IDENT, defaultValue = "HIGH")
    private Code code;

    public Map<String, Code> getCodes() {
        return codes;
    }

    public Map<String, Color> getColors() {
        return colors;
    }

    public Map<String, Perm> getPerms() {
        return perms;
    }

    public Code getCode() {
        return code;
    }
}
