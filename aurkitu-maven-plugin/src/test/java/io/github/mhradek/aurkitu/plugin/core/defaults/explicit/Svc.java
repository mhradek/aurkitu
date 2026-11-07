package io.github.mhradek.aurkitu.plugin.core.defaults.explicit;

import io.github.mhradek.aurkitu.annotations.FlatBufferRpcService;

@FlatBufferRpcService(namespace = "")
public interface Svc {
    Thing get(Thing request);
}
