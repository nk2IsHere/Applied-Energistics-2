
package appeng.core.network.clientbound;

import appeng.client.render.effects.ParticleTypes;
import appeng.core.AEConfig;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record LightningPacket(
        double x,
        double y,
        double z) implements ClientboundPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, LightningPacket> STREAM_CODEC = StreamCodec.ofMember(
            LightningPacket::write,
            LightningPacket::decode);

    public static final Type<LightningPacket> TYPE = CustomAppEngPayload.createType("lightning");

    @Override
    public Type<LightningPacket> type() {
        return TYPE;
    }

    public static LightningPacket decode(RegistryFriendlyByteBuf stream) {
        var x = stream.readFloat();
        var y = stream.readFloat();
        var z = stream.readFloat();
        return new LightningPacket(x, y, z);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeFloat((float) x);
        data.writeFloat((float) y);
        data.writeFloat((float) z);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handleOnClient(Player player) {
        try {
            if (AEConfig.instance().isEnableEffects()) {
                player.getCommandSenderWorld().addParticle(ParticleTypes.LIGHTNING, this.x, this.y, this.z, 0.0f, 0.0f,
                        0.0f);
            }
        } catch (Exception ignored) {
        }
    }
}
