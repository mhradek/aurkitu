package com.michaelhradek.aurkitu.core.output;

/**
 * These are the possible field types when defining a TypeDeclaration. FieldType.IDENT means that the field is a class which
 * ins't one of the other supported field types. IDENT is short for `Identifier`. ARRAY is similar to IDENT however, an array
 * may also be composed or have a type of an IDENT. For example, String[] or List&lt;String&gt;. MAP is a special type which
 * allows the use translates a Java Map to a List[GeneratedObject] and back.
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
