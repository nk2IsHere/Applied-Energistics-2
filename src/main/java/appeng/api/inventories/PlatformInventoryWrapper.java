/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 TeamAppliedEnergistics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.inventories;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

/**
 * Wraps an inventory implementing the platforms standard inventory interface (i.e. IItemHandler on Forge) such that it
 * can be used as an {@link InternalInventory}.
 */
public class PlatformInventoryWrapper implements InternalInventory {
    private final SlottedStorage<ItemVariant> handler;

    public PlatformInventoryWrapper(Storage<ItemVariant> handler) {
        this.handler = (SlottedStorage<ItemVariant>) handler;
    }

    @Override
    public Storage<ItemVariant> toStorage() {
        return handler;
    }

    @Override
    public int size() {
        return handler.getSlotCount();
    }

    @Override
    public int getSlotLimit(int slot) {
        var slotView = handler.getSlot(slot);
        return (int) slotView.getCapacity();
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        var slotView = handler.getSlot(slotIndex);
        var resource = slotView.getResource();
        return resource.toStack((int) slotView.getAmount());
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        try (var tx = Transaction.openOuter()) {
            var slotView = handler.getSlot(slotIndex);
            var variant = ItemVariant.of(stack);
            var amount = stack.getCount();

            if (amount == 0) {
                slotView.extract(variant, slotView.getAmount(), tx);
            } else {
                slotView.insert(variant, amount, tx);
            }

            tx.commit();
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        var slotView = handler.getSlot(slot);
        return slotView.supportsInsertion();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        try (var tx = Transaction.openOuter()) {
            var slotView = handler.getSlot(slot);
            var inserted = slotView.insert(ItemVariant.of(stack), stack.getCount(), tx);

            if (!simulate) {
                tx.commit();
            }

            stack.shrink((int) inserted);
            return stack.isEmpty() ? ItemStack.EMPTY : stack;
        }
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        try (var tx = Transaction.openOuter()) {
            var slotView = handler.getSlot(slot);
            var resource = slotView.getResource();
            var extracted = slotView.extract(resource, amount, tx);

            if (!simulate) {
                tx.commit();
            }

            return resource.toStack((int) extracted);
        }
    }

}
