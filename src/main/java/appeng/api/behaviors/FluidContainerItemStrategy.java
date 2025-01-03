package appeng.api.behaviors;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.util.GenericContainerHelper;
import appeng.util.fluid.FluidSoundHelper;

class FluidContainerItemStrategy
        implements ContainerItemStrategy<AEFluidKey, FluidContainerItemStrategy.Context> {
    @Override
    public @Nullable GenericStack getContainedStack(ItemStack stack) {
        return GenericContainerHelper.getContainedFluidStack(stack);
    }

    @Override
    public @Nullable Context findCarriedContext(Player player, AbstractContainerMenu menu) {
        var fluidCapability = ContainerItemContext.ofPlayerCursor(player, menu).find(FluidStorage.ITEM);
        if (fluidCapability != null) {
            return new CarriedContext(player, menu);
        }
        return null;
    }

    @Override
    public @Nullable Context findPlayerSlotContext(Player player, int slot) {
        var playerInv = PlayerInventoryStorage.of(player.getInventory());
        var fluidCapability = ContainerItemContext.ofPlayerSlot(player, playerInv.getSlot(slot))
                .find(FluidStorage.ITEM);
        if (fluidCapability != null) {
            return new PlayerInvContext(player, slot);
        }

        return null;
    }

    @Override
    public long extract(Context context, AEFluidKey what, long amount, Actionable mode) {
        try (var tx = Transaction.openOuter()) {
            var stack = context.getStack();
            var copy = stack.copyWithCount(1);
            var fluidCapability = ContainerItemContext.withConstant(copy).find(FluidStorage.ITEM);
            if (fluidCapability == null) {
                return 0;
            }

            var extracted = fluidCapability.extract(what.toVariant(), amount, tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return extracted;
        }
    }

    @Override
    public long insert(Context context, AEFluidKey what, long amount, Actionable mode) {
        try (var tx = Transaction.openOuter()) {
            var stack = context.getStack();
            var copy = stack.copyWithCount(1);
            var fluidCapability = ContainerItemContext.withConstant(copy).find(FluidStorage.ITEM);
            if (fluidCapability == null) {
                return 0;
            }

            var filled = fluidCapability.insert(what.toVariant(), amount, tx);
            if (mode == Actionable.MODULATE) {
                tx.commit();
            }
            return filled;
        }
    }

    @Override
    public void playFillSound(Player player, AEFluidKey what) {
        FluidSoundHelper.playFillSound(player, what);
    }

    @Override
    public void playEmptySound(Player player, AEFluidKey what) {
        FluidSoundHelper.playEmptySound(player, what);
    }

    @Override
    public @Nullable GenericStack getExtractableContent(Context context) {
        return getContainedStack(context.getStack());
    }

    interface Context {
        ItemStack getStack();

        void setStack(ItemStack stack);

        void addOverflow(ItemStack stack);
    }

    private record CarriedContext(Player player, AbstractContainerMenu menu) implements Context {
        @Override
        public ItemStack getStack() {
            return menu.getCarried();
        }

        @Override
        public void setStack(ItemStack stack) {
            menu.setCarried(stack);
        }

        public void addOverflow(ItemStack stack) {
            if (menu.getCarried().isEmpty()) {
                menu.setCarried(stack);
            } else {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    private record PlayerInvContext(Player player, int slot) implements Context {
        @Override
        public ItemStack getStack() {
            return player.getInventory().getItem(slot);
        }

        @Override
        public void setStack(ItemStack stack) {
            player.getInventory().setItem(slot, stack);
        }

        public void addOverflow(ItemStack stack) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }
}
