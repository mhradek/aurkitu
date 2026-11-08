package io.github.mhradek.aurkitu.annotations;

import io.github.mhradek.aurkitu.annotations.types.FieldType;

import java.lang.annotation.*;

/**
 * Changes how one field is written: its type, default and FlatBuffers attributes. Fields need it only
 * when the defaults are not enough; every field of a {@link FlatBufferTable} class is written anyway.
 *
 * <pre>{@code
 * @FlatBufferFieldOptions(fieldType = FieldType.UINT, defaultValue = "3", fieldId = 2)
 * int retries;   // retries:uint = 3 (id: 2);
 * }</pre>
 *
 * <p>An option that does not apply to the field (for example {@code required} on an {@code int}, or
 * {@code key} on a second field) is reported as a validation issue and left out, so the schema still
 * compiles. Each check matches what {@code flatc} accepts.</p>
 *
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
public @interface FlatBufferFieldOptions {

    /**
     * The schema type. A scalar or {@code STRING} replaces the type inferred from Java, which is how unsigned
     * types are written ({@code int} as {@code uint}) and how unsupported types such as {@code Instant}
     * become strings; on a collection or array it sets the element type. {@code IDENT}, {@code ARRAY} and
     * {@code MAP} keep the inferred type.
     */
    FieldType fieldType();

    /**
     * Refer to the field's class by its fully qualified Java name, with the Java package as the namespace
     * (e.g. {@code com.example.Point}), instead of by its simple name.
     */
    boolean useFullName() default false;

    /**
     * The default, written as {@code = value}: a number for scalars, {@code true}/{@code false} for
     * {@code bool}, a constant name or value for enums, or {@code null} for an optional scalar or enum.
     * Only table fields of scalar or enum type can have one, and not together with {@code required};
     * a value that does not fit the type is reported and left out. A table field whose enum has no
     * {@code 0} value needs one, because flatc's implicit default is 0. Empty to omit.
     */
    String defaultValue() default "";

    /**
     * Writes {@code (required)}: readers can rely on the field being set. Table fields of string, vector,
     * table or struct type only.
     */
    boolean required() default false;

    /**
     * Writes {@code (deprecated)}: the field keeps its slot but no accessors are generated. Table fields only.
     */
    boolean deprecated() default false;

    /**
     * Writes {@code (id: N)}, fixing the field's slot so fields can be reordered safely. Give every field
     * of the table an id or none; they must be unique and contiguous from 0, and a union field takes two
     * (the hidden {@code _type} field uses the one before it). -1 to omit.
     */
    int fieldId() default -1;

    /**
     * Writes {@code (key)}: the field vectors of this table are sorted and searched by. One per table, on a
     * string, scalar, enum or struct field that is not deprecated.
     */
    boolean key() default false;

    /**
     * Writes {@code (hash: "algorithm")}, so JSON can give a string that is hashed into the field:
     * {@code fnv1_32} or {@code fnv1a_32} on an {@code int}/{@code uint} field, {@code fnv1_64} or
     * {@code fnv1a_64} on a {@code long}/{@code ulong} field, or on vectors of them. Empty to omit.
     */
    String hash() default "";

    /**
     * Writes {@code (nested_flatbuffer: "Table")}: the field, a {@code [ubyte]} ({@code byte[]}), holds
     * a whole buffer whose root is that table, which must be declared in the schema. Empty to omit.
     */
    String nestedFlatbuffer() default "";

    /** Writes {@code (flexbuffer)}: the field, a {@code [ubyte]} ({@code byte[]}), holds FlexBuffer data. */
    boolean flexbuffer() default false;

    /**
     * Writes {@code (force_align: N)} on a vector, aligning its data to N bytes, a power of two. On any other
     * field flatc ignores it, so it is left out with a warning. 0 to omit.
     */
    int forceAlign() default 0;

    /**
     * Structs only: writes the field as a fixed-length array {@code [type:N]}, e.g. {@code float[]} with 3
     * as {@code [float:3]}. In a table it is written as a vector and reported. 0 to omit.
     */
    int fixedLength() default 0;

    /**
     * Custom attributes, e.g. {@code "priority: 1"} or {@code "help: \"text\""}, written after the
     * field. Each name is declared in the schema with {@code attribute "name";}.
     *
     * @since 1.1.0
     */
    String[] attributes() default {};
}
