package io.github.mhradek.aurkitu.plugin.core.mapkeys;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Map key types with typedMapKeys; see {@code MapKeysTest}. */
@FlatBufferTable(rootType = true)
public class Lookup {
    private Map<Long, Integer> byLong;
    private Map<Boolean, Integer> byFlag;
    private Map<UUID, String> byUuid;
    private Map<Color, Integer> byColor;
    private Map<Pos, Integer> byPos;
    private Map<Item, Integer> byItem;
    private Map<List<String>, Integer> byList;
    private Map<Map<String, String>, Integer> byMap;
    private Map<byte[], Integer> byBytes;
    private Map<Object, Integer> byObject;
    private Map<Code, Integer> byCode;
    private Map<String, Code> codeByName;

    public Map<Long, Integer> getByLong() {
        return byLong;
    }

    public Map<Boolean, Integer> getByFlag() {
        return byFlag;
    }

    public Map<UUID, String> getByUuid() {
        return byUuid;
    }

    public Map<Color, Integer> getByColor() {
        return byColor;
    }

    public Map<Pos, Integer> getByPos() {
        return byPos;
    }

    public Map<Item, Integer> getByItem() {
        return byItem;
    }

    public Map<List<String>, Integer> getByList() {
        return byList;
    }

    public Map<Map<String, String>, Integer> getByMap() {
        return byMap;
    }

    public Map<byte[], Integer> getByBytes() {
        return byBytes;
    }

    public Map<Object, Integer> getByObject() {
        return byObject;
    }

    public Map<Code, Integer> getByCode() {
        return byCode;
    }

    public Map<String, Code> getCodeByName() {
        return codeByName;
    }
}
