/**
 * Annotations that describe a FlatBuffers schema in Java. The {@code aurkitu-maven-plugin} scans the
 * annotated classes and writes the schema ({@code .fbs}) for {@code flatc}.
 *
 * <pre>{@code
 * @FlatBufferTable(rootType = true)
 * public class Monster {
 *     private long id;
 *     @FlatBufferFieldOptions(fieldType = FieldType.STRING, required = true)
 *     private String name;
 *     private Color color;
 *     private List<String> tags;
 *     @FlatBufferIgnore
 *     private transient Object cache;
 * }
 *
 * @FlatBufferEnum
 * public enum Color { RED, GREEN, BLUE }
 * }</pre>
 *
 * is written as:
 *
 * <pre>
 * enum Color : byte {
 *   RED,
 *   GREEN,
 *   BLUE,
 * }
 *
 * table Monster {
 *   id:long;
 *   name:string (required);
 *   color:Color;
 *   tags:[string];
 * }
 *
 * root_type Monster;
 * </pre>
 *
 * <ul>
 *   <li>{@link io.github.mhradek.aurkitu.annotations.FlatBufferTable}: a table or struct.</li>
 *   <li>{@link io.github.mhradek.aurkitu.annotations.FlatBufferEnum}: an enum or union.</li>
 *   <li>{@link io.github.mhradek.aurkitu.annotations.FlatBufferFieldOptions}: a field's type, default
 *       and attributes.</li>
 *   <li>{@link io.github.mhradek.aurkitu.annotations.FlatBufferComment}: documentation copied into
 *       generated code.</li>
 *   <li>{@link io.github.mhradek.aurkitu.annotations.FlatBufferIgnore}: leave something out.</li>
 *   <li>{@link io.github.mhradek.aurkitu.annotations.FlatBufferRpcService}: an {@code rpc_service}.</li>
 * </ul>
 *
 * <p>Classes, enums and fields can have any visibility; the annotation is what puts a type in the schema.</p>
 *
 * <p>Java types map to schema types as follows: primitives and their wrappers to the scalar of the same
 * name; {@code String} to {@code string}; {@code byte[]} to {@code [ubyte]}; other arrays and collections
 * to vectors; {@code Map<String, V>} to a vector of a generated key/value table; and annotated classes
 * to references.</p>
 */
package io.github.mhradek.aurkitu.annotations;
