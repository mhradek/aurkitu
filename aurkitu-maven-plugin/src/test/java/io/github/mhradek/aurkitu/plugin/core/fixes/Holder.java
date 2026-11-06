package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FlatBufferTable(rootType = true)
public class Holder {
    private String text;
    private UUID uuid;
    private Instant when;
    private PlayerList players;
    private ArrayList<Integer> ids;
    private List<String> names;
    private Map<String, Long> counts;
    private int[][] grid;
    private List<List<String>> nested;
    private Outer.Inner inner;
    private Audit audit;

    @FlatBufferComment(comment = "costs $5")
    @FlatBufferFieldOptions(fieldType = FieldType.STRING, defaultValue = "")
    private String price;
}
