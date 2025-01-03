package appeng.blockentity.storage;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.blockentity.AEBaseBlockEntity;

public class SkyStoneTankBlockEntity extends AEBaseBlockEntity {

    public static final int BUCKET_CAPACITY = 16;

    private final SingleFluidStorage storage = new SingleFluidStorage() {

        @Override
        protected long getCapacity(FluidVariant variant) {
            return FluidConstants.BUCKET * BUCKET_CAPACITY;
        }

        @Override
        protected void onFinalCommit() {
            SkyStoneTankBlockEntity.this.markForUpdate();
            SkyStoneTankBlockEntity.this.setChanged();
        }
    };

    public SkyStoneTankBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        var tankNbt = new CompoundTag();
        storage.writeNbt(tankNbt, registries);
        if (!tankNbt.isEmpty()) {
            data.put("content", tankNbt);
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        storage.readNbt(data.getCompound("tank"), registries);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        return FluidStorageUtil.interactWithFluidStorage(storage, player, hand);
    }

    public Storage<FluidVariant> getStorage(Direction direction) {
        return storage;
    }

    public SingleVariantStorage<FluidVariant> getStorage() {
        return storage;
    }

    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean ret = super.readFromStream(data);
        storage.readNbt(data.readNbt(), data.registryAccess());
        return ret;
    }

    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        var tag = new CompoundTag();
        storage.writeNbt(tag, data.registryAccess());
        data.writeNbt(tag);
    }
}
