package appeng.core.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.entity.player.Player;

public interface ClientboundPacket extends CustomAppEngPayload {
    Logger LOG = LoggerFactory.getLogger(ClientboundPacket.class);

    default void handleOnClient(ClientPlayNetworking.Context context) {
        var client = context.client();
        LOG.info("Handling packet {} on client", this);
        client.execute(() -> handleOnClient(context.player()));
    }

    default void handleOnClient(Player player) {
        throw new AbstractMethodError("Unimplemented method on " + getClass());
    }
}
