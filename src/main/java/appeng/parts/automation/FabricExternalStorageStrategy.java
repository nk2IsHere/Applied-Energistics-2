package appeng.parts.automation;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import appeng.api.behaviors.ExternalStorageStrategy;
import appeng.api.storage.MEStorage;

public class FabricExternalStorageStrategy<T, S> implements ExternalStorageStrategy {
    private final BlockApiCache<T, Direction> apiCache;
    private final HandlerStrategy<T, S> conversion;
    private final Direction fromSide;

    public FabricExternalStorageStrategy(BlockApiLookup<T, Direction> apiLookup,
             HandlerStrategy<T, S> conversion,
             ServerLevel level,
             BlockPos fromPos,
             Direction fromSide) {
        this.apiCache = BlockApiCache.create(apiLookup, level, fromPos);
        this.conversion = conversion;
        this.fromSide = fromSide;
    }

    @Nullable
    @Override
    public MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback) {
        var storage = apiCache.find(fromSide);
        if (storage == null) {
            return null;
        }

        var result = conversion.getFacade(storage);
        result.setChangeListener(injectOrExtractCallback);
        result.setExtractableOnly(extractableOnly);
        return result;
    }

    public static ExternalStorageStrategy createItem(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricExternalStorageStrategy<>(
                ItemStorage.SIDED,
                HandlerStrategy.ITEMS,
                level,
                fromPos,
                fromSide);
    }

    public static ExternalStorageStrategy createFluid(ServerLevel level, BlockPos fromPos, Direction fromSide) {
        return new FabricExternalStorageStrategy<>(
                FluidStorage.SIDED,
                HandlerStrategy.FLUIDS,
                level,
                fromPos,
                fromSide);
    }
}
