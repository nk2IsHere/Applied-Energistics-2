package appeng.me.storage;

import java.util.Set;

import com.google.common.primitives.Ints;

import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import dev.architectury.fluid.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.core.AELog;
import appeng.core.localization.GuiText;

/**
 * Adapts external platform storage to behave like an {@link MEStorage}.
 */
public abstract class ExternalStorageFacade implements MEStorage {
    /**
     * Clamp reported values to avoid overflows when amounts get too close to Long.MAX_VALUE.
     */
    private static final long MAX_REPORTED_AMOUNT = 1L << 42;

    @Nullable
    private Runnable changeListener;

    protected boolean extractableOnly;

    public void setChangeListener(@Nullable Runnable listener) {
        this.changeListener = listener;
    }

    public abstract int getSlots();

    @Nullable
    public abstract GenericStack getStackInSlot(int slot);

    public abstract AEKeyType getKeyType();

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        var inserted = insertExternal(what, Ints.saturatedCast(amount), mode);
        if (inserted > 0 && mode == Actionable.MODULATE) {
            if (this.changeListener != null) {
                this.changeListener.run();
            }
        }
        return inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        var extracted = extractExternal(what, Ints.saturatedCast(amount), mode);
        if (extracted > 0 && mode == Actionable.MODULATE) {
            if (this.changeListener != null) {
                this.changeListener.run();
            }
        }
        return extracted;
    }

    @Override
    public Component getDescription() {
        return GuiText.ExternalStorage.text(AEKeyType.fluids().getDescription());
    }

    protected abstract int insertExternal(AEKey what, int amount, Actionable mode);

    protected abstract int extractExternal(AEKey what, int amount, Actionable mode);

    public abstract boolean containsAnyFuzzy(Set<AEKey> keys);

    public static ExternalStorageFacade ofFluid(Storage<FluidVariant> handler) {
        return new FluidHandlerFacade(handler);
    }

    public static ExternalStorageFacade ofItem(Storage<ItemVariant> handler) {
        return new ItemHandlerFacade(handler);
    }

    public void setExtractableOnly(boolean extractableOnly) {
        this.extractableOnly = extractableOnly;
    }

    private static class ItemHandlerFacade extends ExternalStorageFacade {
        private final SlottedStorage<ItemVariant> handler;

        public ItemHandlerFacade(Storage<ItemVariant> handler) {
            if(!(handler instanceof SlottedStorage<ItemVariant> slottedStorage)) {
                throw new IllegalArgumentException("Item handler must be slotted");
            }

            this.handler = slottedStorage;
        }

        @Override
        public int getSlots() {
            return handler.getSlotCount();
        }

        @Nullable
        @Override
        public GenericStack getStackInSlot(int slot) {
            return GenericStack.fromItemStack(getItemStackInSlot(slot));
        }

        private ItemStack getItemStackInSlot(int slot) {
            var slotView = handler.getSlot(slot);
            var amount = (int) slotView.getAmount();
            return slotView.getResource().toStack(amount);
        }

        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.items();
        }

        @Override
        public int insertExternal(AEKey what, int amount, Actionable mode) {
            if (!(what instanceof AEItemKey itemKey)) {
                return 0;
            }

            ItemStack orgInput = itemKey.toStack(Ints.saturatedCast(amount));
            ItemStack remaining = orgInput.copy();

            int slotCount = getSlots();
            boolean simulate = mode == Actionable.SIMULATE;

            // This uses a brute force approach and tries to jam it in every slot the inventory exposes.
            try (var tx = Transaction.openOuter()) {
                for (int i = 0; i < slotCount && !remaining.isEmpty(); i++) {
                    var slotView = handler.getSlot(i);
                    var insertedCount = (int) slotView.insert(ItemVariant.of(remaining), remaining.getCount(), tx);
                    remaining.shrink(insertedCount);
                }

                if (!simulate) {
                    tx.commit();
                }
            }

            // At this point, we still have some items left...
            if (remaining.equals(orgInput)) {
                // The stack remained unmodified, target inventory is full
                return 0;
            }

            return amount - remaining.getCount();
        }

        @Override
        public int extractExternal(AEKey what, int amount, Actionable mode) {
            if (!(what instanceof AEItemKey itemKey)) {
                return 0;
            }

            int remainingSize = Ints.saturatedCast(amount);

            // Use this to gather the requested items
            ItemStack gathered = ItemStack.EMPTY;

            final boolean simulate = mode == Actionable.SIMULATE;

            try (var tx = Transaction.openOuter()) {
                for (int i = 0; i < getSlots(); i++) {
                    ItemStack stackInInventorySlot = getItemStackInSlot(i);

                    if (!itemKey.matches(stackInInventorySlot)) {
                        continue;
                    }

                    ItemStack extracted;
                    int stackSizeCurrentSlot = stackInInventorySlot.getCount();
                    int remainingCurrentSlot = Math.min(remainingSize, stackSizeCurrentSlot);

                    // We have to loop here because according to the docs, the handler shouldn't return a stack with
                    // size > maxSize, even if we request more. So even if it returns a valid stack, it might have more
                    // stuff.
                    do {
                        var slotView = handler.getSlot(i);
                        var extractedCount = (int) slotView.extract(slotView.getResource(), remainingCurrentSlot, tx);
                        extracted = slotView.getResource().toStack(extractedCount);

                        if (!extracted.isEmpty()) {
                            // In order to guard against broken IItemHandler implementations, we'll try to guess if the
                            // returned
                            // stack (especially in simulate mode) is the same that was returned by getStackInSlot. This is
                            // obviously not a precise science, but it would catch the previous Forge bug:
                            // https://github.com/MinecraftForge/MinecraftForge/pull/6580
                            if (extracted == stackInInventorySlot) {
                                extracted = extracted.copy();
                            }

                            if (extracted.getCount() > remainingCurrentSlot) {
                                // Something broke. It should never return more than we requested...
                                // We're going to silently eat the remainder
                                AELog.warn(
                                    "Mod that provided item handler %s is broken. Returned %s items while only requesting %d.",
                                    handler.getClass().getName(),
                                    extracted.toString(),
                                    remainingCurrentSlot
                                );
                                extracted.setCount(remainingCurrentSlot);
                            }

                            // Heuristic for simulation: looping in case of simulations is pointless, since the state of the
                            // underlying inventory does not change after a simulated extraction. To still support
                            // inventories
                            // that report stacks that are larger than maxStackSize, we use this heuristic
                            if (simulate && extracted.getCount() == extracted.getMaxStackSize()
                                && remainingCurrentSlot > extracted.getMaxStackSize()) {
                                extracted.setCount(remainingCurrentSlot);
                            }

                            // We're just gonna use the first stack we get our hands on as the template for the rest.
                            if (gathered.isEmpty()) {
                                gathered = extracted;
                            } else {
                                gathered.grow(extracted.getCount());
                            }
                            remainingCurrentSlot -= extracted.getCount();
                        }
                    } while (!simulate && !extracted.isEmpty() && remainingCurrentSlot > 0);

                    remainingSize -= stackSizeCurrentSlot - remainingCurrentSlot;

                    // Done?
                    if (remainingSize <= 0) {
                        break;
                    }
                }

                if (!simulate) {
                    tx.commit();
                }
            }

            if (!gathered.isEmpty()) {
                return gathered.getCount();
            }

            return 0;
        }

        @Override
        public boolean containsAnyFuzzy(Set<AEKey> keys) {
            for (int i = 0; i < getSlots(); i++) {
                var what = AEItemKey.of(getItemStackInSlot(i));
                if (what != null) {
                    if (keys.contains(what.dropSecondary())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            for (int i = 0; i < getSlots(); i++) {
                // Skip resources that cannot be extracted if that filter was enabled
                var stack = getItemStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }

                if (extractableOnly) {
                    try (var tx = Transaction.openOuter()) {
                        var slotView = handler.getSlot(i);
                        var extracted = slotView.extract(slotView.getResource(), 1, tx);
                        if (extracted == 0L) {
                            var extractedAll = slotView.extract(slotView.getResource(), stack.getCount(), tx);
                            if (extractedAll == 0L) {
                                continue;
                            }
                        }
                    }
                }

                out.add(AEItemKey.of(stack), stack.getCount());
            }
        }
    }

    private static class FluidHandlerFacade extends ExternalStorageFacade {
        private final SlottedStorage<FluidVariant> handler;

        public FluidHandlerFacade(Storage<FluidVariant> handler) {
            if(!(handler instanceof SlottedStorage<FluidVariant> slottedStorage)) {
                throw new IllegalArgumentException("Fluid handler must be slotted");
            }

            this.handler = slottedStorage;
        }

        @Override
        public int getSlots() {
            return handler instanceof SlottedStorage<FluidVariant> slotted ? slotted.getSlotCount() : 1;
        }

        @Nullable
        @Override
        public GenericStack getStackInSlot(int slot) {
            var slotView = handler.getSlot(slot);
            var fluid = slotView.getResource();
            var amount = (int) slotView.getAmount();
            return GenericStack.fromFluidStack(FluidStack.create(fluid.getFluid(), amount));
        }

        private FluidStack getFluidInTank(int slot) {
            var slotView = handler.getSlot(slot);
            var fluid = slotView.getResource();
            var amount = (int) slotView.getAmount();
            return FluidStack.create(fluid.getFluid(), amount);
        }

        @Override
        public AEKeyType getKeyType() {
            return AEKeyType.fluids();
        }

        @Override
        protected int insertExternal(AEKey what, int amount, Actionable mode) {
            System.out.println("FluidHandlerFacade.insertExternal " + what + " " + amount + " " + mode);
            if (!(what instanceof AEFluidKey fluidKey)) {
                return 0;
            }

            try (var tx = Transaction.openOuter()) {
                var inserted = handler.insert(fluidKey.toVariant(), amount, tx);
                if (inserted == 0) {
                    return 0;
                }

                if (mode == Actionable.MODULATE) {
                    tx.commit();
                }

                return (int) inserted;
            }
        }

        @Override
        public int extractExternal(AEKey what, int amount, Actionable mode) {
            if (!(what instanceof AEFluidKey fluidKey)) {
                return 0;
            }

            try (var tx = Transaction.openOuter()) {
                var extracted = handler.extract(fluidKey.toVariant(), amount, tx);
                if (extracted == 0) {
                    return 0;
                }

                if (mode == Actionable.MODULATE) {
                    tx.commit();
                }

                return (int) extracted;
            }
        }

        @Override
        public boolean containsAnyFuzzy(Set<AEKey> keys) {
            for (int i = 0; i < getSlots(); i++) {
                var what = AEFluidKey.of(getFluidInTank(i));
                if (what != null) {
                    if (keys.contains(what.dropSecondary())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            try (var tx = Transaction.openOuter()) {
                for (int i = 0; i < getSlots(); i++) {
                    // Skip resources that cannot be extracted if that filter was enabled
                    var stack = getFluidInTank(i);
                    if (stack.isEmpty()) {
                        continue;
                    }

                    if (extractableOnly) {
                        var slotView = handler.getSlot(i);
                        var extracted = slotView.extract(FluidVariant.of(stack.getFluid()), stack.getAmount(), tx);
                        if (extracted == 0L) {
                            continue;
                        }
                    }

                    out.add(AEFluidKey.of(stack), stack.getAmount());
                }
            }
        }
    }
}
