package io.github.mhradek.aurkitu.plugin.core.defaults.explicit;

import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

/** Every annotation element spelled out with its default value; must generate exactly what {@code implicit.Thing} does. */
@FlatBufferTable(rootType = false, value = FlatBufferTable.TableStructureType.TABLE, namespace = "",
        originalOrder = false, forceAlign = 0, attributes = {})
public class Thing {
    @FlatBufferFieldOptions(fieldType = FieldType.INT, useFullName = false, defaultValue = "", required = false,
            deprecated = false, fieldId = -1, key = false, hash = "", nestedFlatbuffer = "", flexbuffer = false,
            forceAlign = 0, fixedLength = 0, attributes = {})
    private int count;
    private Integer boxed;
    private Kind kind;
    private Shape shape;
}
