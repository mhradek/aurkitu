package io.github.mhradek.aurkitu.plugin.core.namespaces;

import io.github.mhradek.aurkitu.annotations.FlatBufferRpcService;

@FlatBufferRpcService(namespace = "other")
public interface Svc {
    Only get(Only request);
}
