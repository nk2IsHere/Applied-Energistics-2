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

package appeng.api.crafting;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.base.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.*;

public final class PatternDetailsHelper {
    private static final List<IPatternDetailsDecoder> DECODERS = new CopyOnWriteArrayList<>();

    static {
        // Register support for our own stacks.
        registerDecoder(AEPatternDecoder.INSTANCE);
    }

    public static void registerDecoder(IPatternDetailsDecoder decoder) {
        Objects.requireNonNull(decoder);
        DECODERS.add(decoder);
    }

    /**
     * Creates a new encoded pattern item based on the given decoder. Your mod must register this item and use it, when
     * it encodes its patterns. You do not need to register {@linkplain #registerDecoder an additional decoder} for the
     * returned item.
     */
    public static <T extends IPatternDetails> EncodedPatternItemBuilder<T> encodedPatternItemBuilder(
            EncodedPatternDecoder<T> decoder) {
        return new EncodedPatternItemBuilder<>(decoder);
    }

    /**
     * Convenience method for decoders that do not need access to the level to decode a pattern.
     * 
     * @see #encodedPatternItemBuilder(EncodedPatternDecoder)
     */
    public static <T extends IPatternDetails> EncodedPatternItemBuilder<T> encodedPatternItemBuilder(
            Function<AEItemKey, T> decoder) {
        return new EncodedPatternItemBuilder<>((what, level) -> decoder.apply(what));
    }

    public static boolean isEncodedPattern(ItemStack stack) {
        for (var decoder : DECODERS) {
            if (decoder.isEncodedPattern(stack)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static IPatternDetails decodePattern(AEItemKey what, Level level) {
        for (var decoder : DECODERS) {
            var decoded = decoder.decodePattern(what, level);
            if (decoded != null) {
                return decoded;
            }
        }
        return null;
    }

    @Nullable
    public static IPatternDetails decodePattern(ItemStack stack, Level level) {
        for (var decoder : DECODERS) {
            var decoded = decoder.decodePattern(stack, level);
            if (decoded != null) {
                return decoded;
            }
        }
        return null;
    }

    /**
     * Encodes a processing pattern which represents the ability to convert the given inputs into the given outputs
     * using some process external to the ME system.
     *
     * @param sparseOutputs The first element is considered the primary output and must be present
     * @return A new encoded pattern.
     * @throws IllegalArgumentException If either in or out contain only empty ItemStacks, or no primary output
     */
    public static ItemStack encodeProcessingPattern(List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs) {
        var stack = AEItems.PROCESSING_PATTERN.stack();
        AEProcessingPattern.encode(stack, sparseInputs, sparseOutputs);
        return stack;
    }

    /**
     * Encodes a crafting pattern which represents a Vanilla crafting recipe.
     *
     * @param recipe                The Vanilla crafting recipe to be encoded.
     * @param in                    The items in the crafting grid, which are used to determine what items are supplied
     *                              from the ME system to craft using this pattern.
     * @param out                   What is to be expected as the result of this crafting operation by the ME system.
     * @param allowSubstitutes      Controls whether the ME system will allow the use of equivalent items to craft this
     *                              recipe.
     * @param allowFluidSubstitutes Controls whether the ME system will allow the use of equivalent fluids.
     * @throws IllegalArgumentException If either in or out contain only empty ItemStacks.
     */
    public static ItemStack encodeCraftingPattern(RecipeHolder<CraftingRecipe> recipe, ItemStack[] in,
            ItemStack out, boolean allowSubstitutes, boolean allowFluidSubstitutes) {
        var stack = AEItems.CRAFTING_PATTERN.stack();
        AECraftingPattern.encode(stack, recipe, in, out, allowSubstitutes,
                allowFluidSubstitutes);
        return stack;
    }

    /**
     * Encodes a stonecutting pattern which represents a Vanilla Stonecutter recipe.
     *
     * @param recipe           The Vanilla stonecutter recipe to be encoded.
     * @param in               The input item for the stonecutter, which is used to determine which item is supplied
     *                         from the ME system to craft using this pattern.
     * @param out              The selected output item from the stonecutter recipe. Used to restore the recipe if it is
     *                         renamed later.
     * @param allowSubstitutes Controls whether the ME system will allow the use of equivalent items to craft this
     *                         recipe.
     */
    public static ItemStack encodeStonecuttingPattern(RecipeHolder<StonecutterRecipe> recipe, AEItemKey in,
            AEItemKey out,
            boolean allowSubstitutes) {
        var stack = AEItems.STONECUTTING_PATTERN.stack();
        AEStonecuttingPattern.encode(stack, recipe, in, out, allowSubstitutes);
        return stack;
    }

    /**
     * Encodes a smithing table pattern which represents a Vanilla Smithing Table recipe.
     *
     * @param recipe           The Vanilla smithing table recipe to be encoded.
     * @param template         The template item for the smithing table.
     * @param base             The base item for the smithing table, which is used to determine which item is supplied
     *                         from the ME system to craft using this pattern.
     * @param addition         The additional item for the smithing table, which is used to determine which item is
     *                         supplied from the ME system to craft using this pattern.
     * @param out              The selected output item from the smithing table recipe. Used to restore the recipe if it
     *                         is renamed later.
     * @param allowSubstitutes Controls whether the ME system will allow the use of equivalent items to craft this
     *                         recipe.
     */
    public static ItemStack encodeSmithingTablePattern(RecipeHolder<SmithingRecipe> recipe,
            AEItemKey template,
            AEItemKey base,
            AEItemKey addition,
            AEItemKey out,
            boolean allowSubstitutes) {
        var stack = AEItems.SMITHING_TABLE_PATTERN.stack();
        AESmithingTablePattern.encode(stack, recipe, template, base, addition, out, allowSubstitutes);
        return stack;
    }
}
