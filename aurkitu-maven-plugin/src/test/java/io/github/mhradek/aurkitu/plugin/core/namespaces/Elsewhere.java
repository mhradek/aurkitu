package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** A union in namespace other: its Note member is other.Note. */
@FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.UNION, namespace = "other")
public enum Elsewhere { Note }
