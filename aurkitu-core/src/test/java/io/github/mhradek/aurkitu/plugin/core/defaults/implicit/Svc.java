package io.github.mhradek.aurkitu.plugin.core.defaults.implicit;

import io.github.mhradek.aurkitu.annotations.FlatBufferRpcService;

@FlatBufferRpcService
public interface Svc {
    Thing get(Thing request);
}
