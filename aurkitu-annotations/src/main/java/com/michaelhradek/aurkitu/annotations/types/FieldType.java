package com.michaelhradek.aurkitu.annotations.types;

/**
 * These are the possible field types when defining a TypeDeclaration. FieldType.IDENT means that the field is a class which
 * ins't one of the other supported field types. IDENT is short for `Identifier`. ARRAY is similar to IDENT however, an array
 * may also be composed or have a type of an IDENT. For example, String[] or List&lt;String&gt;. MAP is a special type which
 * allows the use translates a Java Map to a List[GeneratedObject] and back.
 *
 * See https://google.github.io/flatbuffers/md__schemas.html
 *
 * 8 bit: byte ubyte bool
 * 16 bit: short ushort
 * 32 bit: int uint float
 * 64 bit: long ulong double
 *
 * Built-in non-scalar types:
 *    * Vector of any other type (denoted with [type]). Nesting vectors is not supported, instead you can wrap the inner vector in a table.
 *    * string, which may only hold UTF-8 or 7-bit ASCII. For other text encodings or general binary data use vectors ([byte] or [ubyte]) instead.
 *    * References to other tables or structs, enums or unions (see below).
 *
 * @author m.hradek
 */
public enum FieldType {
    BOOL(boolean.class), BYTE(byte.class), UBYTE(byte.class), SHORT(short.class), USHORT(
            short.class), INT(int.class), UINT(int.class), FLOAT(float.class), LONG(long.class), ULONG(
            long.class), DOUBLE(double.class), STRING(String.class), IDENT(null), ARRAY(null), MAP(null);

    public Class<?> targetClass;

    FieldType(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
