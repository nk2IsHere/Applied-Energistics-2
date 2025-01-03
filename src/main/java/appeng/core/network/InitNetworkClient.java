package appeng.core.network;

import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.clientbound.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@Environment(EnvType.CLIENT)
public class InitNetworkClient {

    public static void init() {
        // Clientbound
        clientbound(AssemblerAnimationPacket.TYPE);
        clientbound(BlockTransitionEffectPacket.TYPE);
        clientbound(ClearPatternAccessTerminalPacket.TYPE);
        clientbound(CompassResponsePacket.TYPE);
        clientbound(CraftConfirmPlanPacket.TYPE);
        clientbound(CraftingJobStatusPacket.TYPE);
        clientbound(CraftingStatusPacket.TYPE);
        clientbound(GuiDataSyncPacket.TYPE);
        clientbound(ItemTransitionEffectPacket.TYPE);
        clientbound(LightningPacket.TYPE);
        clientbound(MatterCannonPacket.TYPE);
        clientbound(MEInventoryUpdatePacket.TYPE);
        clientbound(MockExplosionPacket.TYPE);
        clientbound(NetworkStatusPacket.TYPE);
        clientbound(PatternAccessTerminalPacket.TYPE);
        clientbound(SetLinkStatusPacket.TYPE);
        clientbound(ExportedGridContent.TYPE);

        bidirectional(ConfigValuePacket.TYPE);
    }

    private static <T extends ClientboundPacket> void clientbound(CustomPacketPayload.Type<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var packet = (ClientboundPacket) payload;
            packet.handleOnClient(context.client(), context.player());
        });
    }

    private static <T extends ClientboundPacket & ServerboundPacket> void bidirectional(CustomPacketPayload.Type<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var packet = (ClientboundPacket) payload;
            packet.handleOnClient(context.client(), context.player());
        });
    }
}
