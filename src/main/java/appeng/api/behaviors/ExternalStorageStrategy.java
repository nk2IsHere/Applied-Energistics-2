package appeng.api.behaviors;

import appeng.api.stacks.AEKeyType;
import appeng.api.storage.MEStorage;
import appeng.parts.automation.StackWorldBehaviors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Experimental
public interface ExternalStorageStrategy {
    @Nullable
    MEStorage createWrapper(boolean extractableOnly, Runnable injectOrExtractCallback);

    @FunctionalInterface
    interface Factory {
        ExternalStorageStrategy create(ServerLevel level, BlockPos fromPos, Direction fromSide);
    }

    static void register(AEKeyType type, Factory factory) {
        StackWorldBehaviors.registerExternalStorageStrategy(type, factory);
    }
}
