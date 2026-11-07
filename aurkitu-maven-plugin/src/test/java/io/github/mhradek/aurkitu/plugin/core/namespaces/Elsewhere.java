package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** A union in namespace other: its Note member is other.Note. */
@FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.UNION, namespace = "other")
// Union members name their tables, so the constants follow table names.
@SuppressWarnings("PMD.FieldNamingConventions")
public enum Elsewhere { Note }
