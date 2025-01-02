package appeng.core.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ServerboundPacket extends CustomAppEngPayload {
    Logger LOG = LoggerFactory.getLogger(ServerboundPacket.class);

    default void handleOnServer(ServerPlayNetworking.Context context) {
        try(var server = context.server()) {
            LOG.info("Handling packet {} on server", this);
            server.execute(() -> handleOnServer(context.player()));
        }
    }

    void handleOnServer(ServerPlayer player);
}
