package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;
import io.github.mhradek.aurkitu.annotations.types.FieldType;

/** Struct containing a struct, a fixed-length array and an unsigned scalar. */
@FlatBufferComment(comment = "Position, rotation quaternion and render layer")
@FlatBufferTable(FlatBufferTable.TableStructureType.STRUCT)
public class Transform {
    private Vec3 position;

    @FlatBufferFieldOptions(fieldType = FieldType.ARRAY, fixedLength = 4)
    private float[] rotation;

    @FlatBufferFieldOptions(fieldType = FieldType.UBYTE)
    private byte layer;
}
