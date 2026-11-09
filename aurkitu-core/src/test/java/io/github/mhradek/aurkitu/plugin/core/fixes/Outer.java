package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

public class Outer {
    @FlatBufferTable
    public static class Inner {
        private int value;
    }

    /** Non-static inner class: the synthetic this$0 field must not be emitted. */
    @FlatBufferTable
    public class Attached {
        private int value;
    }
}
