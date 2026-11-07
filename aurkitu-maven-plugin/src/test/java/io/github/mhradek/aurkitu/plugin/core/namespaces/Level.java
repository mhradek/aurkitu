package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferEnum;

/** An enum in another namespace. */
@FlatBufferEnum(namespace = "other")
public enum Level { LOW, HIGH }
