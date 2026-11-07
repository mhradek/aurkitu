package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** Members resolve in the union's namespace first: Note is the default-namespace one; Only is the only one. */
@FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.UNION)
// Union members name their tables, so the constants follow table names.
@SuppressWarnings("PMD.FieldNamingConventions")
public enum Mixed { Note, Only }
