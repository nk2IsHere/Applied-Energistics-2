package appeng.mixins;

import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import appeng.hooks.INeighborChangeSensitive;

/**
 * Replicates Forge's callback for non-comparators to get neighboring block changes similar to a comparator.
 */
@Mixin(Level.class)
public abstract class OnNeighborUpdateMixin {

    /**
     * This targets the first getBlockState in the method and injects right after. We want to capture the return value,
     * essentially so that we do not have to get the blockstate again.
     */
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "updateNeighbourForOutputSignal", at = @At(value = "INVOKE_ASSIGN", ordinal = 0, target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    public void triggerOnNeighborChange(BlockPos srcPos, Block srcBlock, CallbackInfo ci,
            @Local(ordinal = 1) BlockPos blockPos) {
        Level world = (Level) (Object) this;
        BlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof INeighborChangeSensitive changeSensitive) {
            changeSensitive.onNeighborChanged(world, blockPos, srcPos);
        }
    }
}
