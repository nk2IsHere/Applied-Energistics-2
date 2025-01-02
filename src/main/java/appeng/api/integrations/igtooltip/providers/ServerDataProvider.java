package appeng.api.integrations.igtooltip.providers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
@ApiStatus.OverrideOnly
@FunctionalInterface
public interface ServerDataProvider<T> {
    void provideServerData(Player player, T object, CompoundTag serverData);
}
