package appeng.core.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.clientbound.*;
import appeng.core.network.serverbound.*;

public class InitNetwork {
    public static void init() {

        // Clientbound
        clientbound(AssemblerAnimationPacket.TYPE, AssemblerAnimationPacket.STREAM_CODEC);
        clientbound(BlockTransitionEffectPacket.TYPE, BlockTransitionEffectPacket.STREAM_CODEC);
        clientbound(ClearPatternAccessTerminalPacket.TYPE, ClearPatternAccessTerminalPacket.STREAM_CODEC);
        clientbound(CompassResponsePacket.TYPE, CompassResponsePacket.STREAM_CODEC);
        clientbound(CraftConfirmPlanPacket.TYPE, CraftConfirmPlanPacket.STREAM_CODEC);
        clientbound(CraftingJobStatusPacket.TYPE, CraftingJobStatusPacket.STREAM_CODEC);
        clientbound(CraftingStatusPacket.TYPE, CraftingStatusPacket.STREAM_CODEC);
        clientbound(GuiDataSyncPacket.TYPE, GuiDataSyncPacket.STREAM_CODEC);
        clientbound(ItemTransitionEffectPacket.TYPE, ItemTransitionEffectPacket.STREAM_CODEC);
        clientbound(LightningPacket.TYPE, LightningPacket.STREAM_CODEC);
        clientbound(MatterCannonPacket.TYPE, MatterCannonPacket.STREAM_CODEC);
        clientbound(MEInventoryUpdatePacket.TYPE, MEInventoryUpdatePacket.STREAM_CODEC);
        clientbound(MockExplosionPacket.TYPE, MockExplosionPacket.STREAM_CODEC);
        clientbound(NetworkStatusPacket.TYPE, NetworkStatusPacket.STREAM_CODEC);
        clientbound(PatternAccessTerminalPacket.TYPE, PatternAccessTerminalPacket.STREAM_CODEC);
        clientbound(SetLinkStatusPacket.TYPE, SetLinkStatusPacket.STREAM_CODEC);
        clientbound(ExportedGridContent.TYPE, ExportedGridContent.STREAM_CODEC);

        // Serverbound
        serverbound(ColorApplicatorSelectColorPacket.TYPE, ColorApplicatorSelectColorPacket.STREAM_CODEC);
        serverbound(RequestClosestMeteoritePacket.TYPE, RequestClosestMeteoritePacket.STREAM_CODEC);
        serverbound(ConfigButtonPacket.TYPE, ConfigButtonPacket.STREAM_CODEC);
        serverbound(ConfirmAutoCraftPacket.TYPE, ConfirmAutoCraftPacket.STREAM_CODEC);
        serverbound(FillCraftingGridFromRecipePacket.TYPE, FillCraftingGridFromRecipePacket.STREAM_CODEC);
        serverbound(GuiActionPacket.TYPE, GuiActionPacket.STREAM_CODEC);
        serverbound(HotkeyPacket.TYPE, HotkeyPacket.STREAM_CODEC);
        serverbound(InventoryActionPacket.TYPE, InventoryActionPacket.STREAM_CODEC);
        serverbound(MEInteractionPacket.TYPE, MEInteractionPacket.STREAM_CODEC);
        serverbound(MouseWheelPacket.TYPE, MouseWheelPacket.STREAM_CODEC);
        serverbound(PartLeftClickPacket.TYPE, PartLeftClickPacket.STREAM_CODEC);
        serverbound(SelectKeyTypePacket.TYPE, SelectKeyTypePacket.STREAM_CODEC);
        serverbound(SwapSlotsPacket.TYPE, SwapSlotsPacket.STREAM_CODEC);
        serverbound(SwitchGuisPacket.TYPE, SwitchGuisPacket.STREAM_CODEC);
        serverbound(UpdateHoldingCtrlPacket.TYPE, UpdateHoldingCtrlPacket.STREAM_CODEC);

        // Bidirectional
        bidirectional(ConfigValuePacket.TYPE, ConfigValuePacket.STREAM_CODEC);
    }

    private static <T extends ClientboundPacket> void clientbound(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);

        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var packet = (ClientboundPacket) payload;
            packet.handleOnClient(context);
        });
    }

    private static <T extends ServerboundPacket> void serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);

        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var packet = (ServerboundPacket) payload;
            packet.handleOnServer(context);
        });
    }

    private static <T extends ServerboundPacket & ClientboundPacket> void bidirectional(
            CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.playC2S().register(type, codec);
        PayloadTypeRegistry.playS2C().register(type, codec);

        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var packet = (ClientboundPacket) payload;
            packet.handleOnClient(context);
        });

        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> {
            var packet = (ServerboundPacket) payload;
            packet.handleOnServer(context);
        });
    }
}
