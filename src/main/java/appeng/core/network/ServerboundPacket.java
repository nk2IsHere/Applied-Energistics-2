package appeng.core.network;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.level.ServerPlayer;

public interface ServerboundPacket extends CustomAppEngPayload {
    Logger LOG = LoggerFactory.getLogger(ServerboundPacket.class);

    default void handleOnServer(MinecraftServer server, ServerPlayer player) {
        LOG.info("Handling packet {} on server", this);
        server.execute(() -> handleOnServer(player));
    }

    void handleOnServer(ServerPlayer player);
}
