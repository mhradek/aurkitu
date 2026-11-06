package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.FlatBufferIgnore;
import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

/** Unannotated nested types of an annotated table are declared automatically (as in 0.0.x). */
@FlatBufferTable
public class WithNested {
    private Mode mode;
    private Detail detail;

    public enum Mode { ON, OFF }

    public static class Detail {
        private int level;
    }

    /** Opted out. */
    @FlatBufferIgnore
    public static class Hidden {
        private int secret;
    }

    /** Interfaces are not types. */
    public interface Marker {}
}
