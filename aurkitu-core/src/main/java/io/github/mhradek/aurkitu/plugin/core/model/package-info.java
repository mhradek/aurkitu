/**
 * Typed model of a FlatBuffers schema.
 *
 * <p>Generation runs in three steps: a reader turns scanned Java classes into this model,
 * {@code SchemaValidator} checks it (returning a corrected copy plus issues), and each element
 * then writes its own schema syntax through a {@link io.github.mhradek.aurkitu.plugin.core.model.SchemaWriter}.
 * Decisions are made on these types, never by inspecting generated text.</p>
 */
package io.github.mhradek.aurkitu.plugin.core.model;
