package appeng.client;

import appeng.core.network.ServerboundPacket;
import appeng.core.network.serverbound.HotkeyPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

public record Hotkey(String name, KeyMapping mapping) {
    public void check() {
        while (mapping().consumeClick()) {
            ServerboundPacket message = new HotkeyPacket(this);
            ClientPlayNetworking.send(message);
        }
    }
}
