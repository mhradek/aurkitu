package io.github.mhradek.aurkitu.plugin.core.fixes;

import io.github.mhradek.aurkitu.annotations.*;
import io.github.mhradek.aurkitu.annotations.types.*;

@FlatBufferComment(comment = "Storage service")
@FlatBufferRpcService
public interface Store {
    @FlatBufferComment(comment = "Saves players")
    Checkpoint put(PlayerList players);

    Checkpoint twoArguments(PlayerList a, PlayerList b);

    @FlatBufferIgnore
    Checkpoint ignored(PlayerList players);

    default Checkpoint notAbstract(PlayerList players) {
        return null;
    }
}
