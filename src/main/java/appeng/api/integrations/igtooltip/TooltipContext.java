package appeng.api.integrations.igtooltip;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public record TooltipContext(CompoundTag serverData, Vec3 hitLocation, Player player) {
    public HolderLookup.Provider registries() {
        return player.registryAccess();
    }
}
