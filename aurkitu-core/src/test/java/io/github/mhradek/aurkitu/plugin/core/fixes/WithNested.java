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

        public int getLevel() {
            return level;
        }

        /** Two levels below WithNested (Detail is not annotated): not declared. */
        public static class Deeper {
            private int depth;

            public int getDepth() {
                return depth;
            }
        }
    }

    /** Opted out. */
    @FlatBufferIgnore
    public static class Hidden {
        private int secret;
    }

    /** Interfaces are not types. */
    public interface Marker {}

    /** Annotated nested types are declared once, as annotated types. */
    @FlatBufferTable
    public static class Tagged {
        private int tag;
    }

    /** Anonymous classes are not types. */
    static final Runnable ANONYMOUS = new Runnable() {
        @Override
        public void run() {
            // nothing
        }
    };
}
