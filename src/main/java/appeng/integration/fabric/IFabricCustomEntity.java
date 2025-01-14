package appeng.integration.fabric;

import net.minecraft.network.FriendlyByteBuf;

public interface IFabricCustomEntity {

    default void writeAdditionalSpawnData(FriendlyByteBuf buf) {
    }

    default void readAdditionalSpawnData(FriendlyByteBuf buf) {
    }
}
