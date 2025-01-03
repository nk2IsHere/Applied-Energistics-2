package appeng.hooks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/**
 * Replicates a Forge hook which allows non-comparator blocks to react to changes in adjacent blocks that would
 * otherwise only be visible to comparators (especially inventory changes).
 */
public interface INeighborChangeSensitive {

    // This is usually a Forge extension. We replace it using a Mixin.
    void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor);
}
