package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

/** With optionalScalars, boxed scalars without a default or required become "= null". */
@FlatBufferTable
public class Optionals {
    private Integer maybe;
    private Boolean flag;
    private int plain;
    @FlatBufferFieldOptions(fieldType = FieldType.INT, defaultValue = "7")
    private Integer withDefault;

    public Integer getMaybe() {
        return maybe;
    }

    public Boolean getFlag() {
        return flag;
    }

    public int getPlain() {
        return plain;
    }

    public Integer getWithDefault() {
        return withDefault;
    }
}
