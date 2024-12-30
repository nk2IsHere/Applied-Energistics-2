package appeng.core.definitions;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class DeferredBlockEntityType<T extends BlockEntity> implements Supplier<BlockEntityType<T>> {
    private final Class<T> blockEntityClass;

    private final RegistrySupplier<BlockEntityType<T>> holder;

    public DeferredBlockEntityType(Class<T> blockEntityClass,
        RegistrySupplier<BlockEntityType<T>> holder) {
        this.blockEntityClass = blockEntityClass;
        this.holder = holder;
    }

    public Class<T> getBlockEntityClass() {
        return blockEntityClass;
    }

    @Override
    public BlockEntityType<T> get() {
        return holder.get();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public T getBlockEntity(BlockGetter level, BlockPos pos) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        return (T) (blockentity != null && blockentity.getType() == holder.get() ? blockentity : null);
    }

}
