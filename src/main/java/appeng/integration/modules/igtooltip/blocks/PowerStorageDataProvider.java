package appeng.integration.modules.igtooltip.blocks;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.core.localization.InGameTooltip;
import appeng.util.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shows stored power and max stored power for an {@link IAEPowerStorage} block entity.
 */
public final class PowerStorageDataProvider implements BodyProvider<BlockEntity>, ServerDataProvider<BlockEntity> {

    /**
     * Power key used for the transferred {@link CompoundTag}
     */
    private static final String TAG_CURRENT_POWER = "currentPower";
    private static final String TAG_MAX_POWER = "maxPower";

    @Override
    public void buildTooltip(BlockEntity object, TooltipContext context, TooltipBuilder tooltip) {
        var tag = context.serverData();
        if (tag.contains(TAG_MAX_POWER, Tag.TAG_DOUBLE)) {
            var currentPower = tag.getDouble(TAG_CURRENT_POWER);
            var maxPower = tag.getDouble(TAG_MAX_POWER);

            var formatCurrentPower = Platform.formatPower(currentPower, false);
            var formatMaxPower = Platform.formatPower(maxPower, false);

            tooltip.addLine(InGameTooltip.Stored.text(formatCurrentPower, formatMaxPower));
        }
    }

    @Override
    public void provideServerData(Player player, BlockEntity object, CompoundTag serverData) {
        if (object instanceof IAEPowerStorage storage) {
            if (storage.getAEMaxPower() > 0) {
                serverData.putDouble(TAG_CURRENT_POWER, storage.getAECurrentPower());
                serverData.putDouble(TAG_MAX_POWER, storage.getAEMaxPower());
            }
        }
    }
}
