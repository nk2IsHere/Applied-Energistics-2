/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package appeng.util.neoforge.util;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ItemStackMap {
    public static final Hash.Strategy<? super ItemStack> TYPE_AND_TAG = new Hash.Strategy<>() {
        public int hashCode(@Nullable ItemStack stack) {
            return ItemStack.hashItemAndComponents(stack);
        }

        public boolean equals(@Nullable ItemStack first, @Nullable ItemStack second) {
            return first == second || first != null && second != null && first.isEmpty() == second.isEmpty() && ItemStack.isSameItemSameComponents(first, second);
        }
    };

    public ItemStackMap() {
    }

    public static <V> Map<ItemStack, V> createTypeAndTagLinkedMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(TYPE_AND_TAG);
    }

    public static <V> Map<ItemStack, V> createTypeAndTagMap() {
        return new Object2ObjectOpenCustomHashMap<>(TYPE_AND_TAG);
    }
}
