package appeng.core.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ClientboundPacket extends CustomAppEngPayload {
    Logger LOG = LoggerFactory.getLogger(ClientboundPacket.class);

    @Environment(EnvType.CLIENT)
    default void handleOnClient(Minecraft client, Player player) {
        LOG.info("Handling packet {} on client", this);
        client.execute(() -> handleOnClient(player));
    }

    default void handleOnClient(Player player) {
        throw new AbstractMethodError("Unimplemented method on " + getClass());
    }
}
