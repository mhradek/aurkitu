package io.github.mhradek.aurkitu.annotations.types;

/**
 * The schema type of a field, set with {@code FlatBufferFieldOptions.fieldType}. Each scalar constant is
 * written as the FlatBuffers type of the same name; Java has no unsigned types, so the unsigned ones are
 * how a Java {@code int} becomes a {@code uint}, for example.
 *
 * <p>{@code IDENT}, {@code ARRAY} and {@code MAP} keep the type inferred from the Java field: a reference to
 * another table, struct, enum or union; a vector for arrays and collections; a generated
 * {@code MapValueSet_*} table of {@code key}/{@code value} entries for maps.</p>
 *
 * @see <a href="https://flatbuffers.dev/schema/">FlatBuffers schema types</a>
 * @author m.hradek
 */
public enum FieldType {
    /** {@code bool}: 8-bit true/false. */
    BOOL(boolean.class),
    /** {@code byte}: 8-bit signed. */
    BYTE(byte.class),
    /** {@code ubyte}: 8-bit unsigned. */
    UBYTE(byte.class),
    /** {@code short}: 16-bit signed. */
    SHORT(short.class),
    /** {@code ushort}: 16-bit unsigned. */
    USHORT(short.class),
    /** {@code int}: 32-bit signed. */
    INT(int.class),
    /** {@code uint}: 32-bit unsigned. */
    UINT(int.class),
    /** {@code float}: 32-bit floating point. */
    FLOAT(float.class),
    /** {@code long}: 64-bit signed. */
    LONG(long.class),
    /** {@code ulong}: 64-bit unsigned. */
    ULONG(long.class),
    /** {@code double}: 64-bit floating point. */
    DOUBLE(double.class),
    /** {@code string}: UTF-8 text. Also makes a type with no schema equivalent (e.g. {@code Instant}) a string. */
    STRING(String.class),
    /** Keep the inferred type: a reference to a declared table, struct, enum or union. */
    IDENT(null),
    /** Keep the inferred type: a vector ({@code [type]}) for an array or collection. */
    ARRAY(null),
    /** Keep the inferred type: a vector of a generated key/value table for a {@code Map}. */
    MAP(null);

    /** The Java type that holds this type's values; null for {@code IDENT}, {@code ARRAY} and {@code MAP}. */
    public Class<?> targetClass;

    FieldType(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
