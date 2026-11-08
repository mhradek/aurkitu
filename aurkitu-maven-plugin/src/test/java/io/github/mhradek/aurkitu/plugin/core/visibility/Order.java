package io.github.mhradek.aurkitu.plugin.core.visibility;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Public root table using non-public types of every kind. */
@FlatBufferTable(rootType = true)
public class Order extends Base {
    long id;
    Hidden hidden;
    Kind kind;
    Line line;
    Note note;

    /** A private nested type, annotated. */
    @FlatBufferTable
    private static class Line {
        int qty;
    }

    /** A private nested type without an annotation: included like any nested type. */
    private static class Note {
        String text;
    }
}
