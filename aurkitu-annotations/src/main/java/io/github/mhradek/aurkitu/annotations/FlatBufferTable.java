package io.github.mhradek.aurkitu.annotations;

import java.lang.annotation.*;

/**
 * Declares a class as a FlatBuffers {@code table} (the default) or {@code struct}. Its fields,
 * including inherited ones, become the schema's fields; mark one {@link FlatBufferIgnore} to leave it
 * out, and use {@link FlatBufferFieldOptions} to change how it is written.
 *
 * <pre>{@code
 * @FlatBufferTable(rootType = true)
 * class Monster { long id; String name; }   // table Monster { id:long; name:string; }  root_type Monster;
 * }</pre>
 *
 * <p>The type is named by its simple Java name ({@code Outer.Inner} becomes {@code Inner}). Classes and
 * enums nested directly in an annotated class are written too, in its namespace, without their own
 * annotation; mark one {@link FlatBufferIgnore} to leave it out.</p>
 *
 * @author m.hradek
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface FlatBufferTable {

    /**
     * Writes {@code root_type} for this table: the type a buffer holds at its top level. A schema needs
     * exactly one. A struct cannot be the root; marking one is reported and ignored.
     */
    boolean rootType() default false;

    /**
     * {@code TABLE} or {@code STRUCT}. Tables can evolve (fields added, deprecated) and have optional fields;
     * structs are fixed-size, can hold only scalars, enums, other structs and fixed-length arrays, and can
     * never change once in use.
     */
    TableStructureType value() default TableStructureType.TABLE;

    /**
     * The schema namespace of this type, overriding the plugin's {@code schemaNamespace}. Unannotated
     * nested types follow it.
     */
    String namespace() default "";

    /**
     * Tables only: keep fields in declaration order in the buffer (original_order).
     *
     * @since 1.1.0
     */
    boolean originalOrder() default false;

    /**
     * Structs only: force the struct's alignment to this power of two (force_align). 0 to omit.
     *
     * @since 1.1.0
     */
    int forceAlign() default 0;

    /**
     * Custom attributes written after the type name, e.g. {@code "priority: 1"}. Each name is declared
     * in the schema with {@code attribute "name";}; it must be an identifier and not a built-in attribute.
     *
     * @since 1.1.0
     */
    String[] attributes() default {};

    /** Whether the class is written as a {@code table} or a {@code struct}. */
    public enum TableStructureType {
        /** {@code table}: fields are optional and can be added later. */
        TABLE,
        /** {@code struct}: fixed layout, every field always present. */
        STRUCT
    }
}
