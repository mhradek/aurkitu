package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** Members resolve in the union's namespace first: Note is the default-namespace one; Only is the only one. */
@FlatBufferEnum(value = FlatBufferEnum.EnumStructureType.UNION)
public enum Mixed { Note, Only }
