package io.github.mhradek.aurkitu.plugin.core.testmodels;

import io.github.mhradek.aurkitu.annotations.FlatBufferTable;

import java.util.Optional;

@FlatBufferTable
public class SampleWithUnsupported {
    private Optional<String> maybe;
}
