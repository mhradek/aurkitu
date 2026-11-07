package com.example.kitchensink;

import io.github.mhradek.aurkitu.annotations.FlatBufferComment;
import io.github.mhradek.aurkitu.annotations.FlatBufferRpcService;

/** Declares an rpc_service; flatc generates code for it with --grpc. */
@FlatBufferComment(comment = "Player lookups")
@FlatBufferRpcService
public interface PlayerService {
    @FlatBufferComment(comment = "Finds a player by item")
    Player find(Item item);

    Versioned version(Player player);
}
