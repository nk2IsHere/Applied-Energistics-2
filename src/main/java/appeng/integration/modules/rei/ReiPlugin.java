/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.integration.modules.rei;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import dev.architectury.event.CompoundEventResult;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.ButtonArea;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;

import appeng.api.config.Actionable;
import appeng.api.config.CondenserOutput;
import appeng.api.features.P2PTunnelAttunementInternal;
import appeng.api.integrations.rei.IngredientConverters;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.InscriberScreen;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.FacadeCreativeTab;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.ItemDefinition;
import appeng.core.localization.GuiText;
import appeng.core.localization.ItemModText;
import appeng.integration.abstraction.ItemListMod;
import appeng.integration.modules.itemlists.CompatLayerHelper;
import appeng.integration.modules.itemlists.ItemPredicates;
import appeng.integration.modules.rei.transfer.EncodePatternTransferHandler;
import appeng.integration.modules.rei.transfer.UseCraftingRecipeTransfer;
import appeng.items.parts.FacadeItem;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.recipes.AERecipeTypes;
import appeng.recipes.entropy.EntropyRecipe;
import appeng.recipes.game.StorageCellUpgradeRecipe;
import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;
import appeng.recipes.transform.TransformRecipe;

public class ReiPlugin implements REIClientPlugin {
    static final ResourceLocation TEXTURE = AppEng.makeId("textures/guis/jei.png");

    public ReiPlugin() {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        IngredientConverters.register(new ItemIngredientConverter());
        IngredientConverters.register(new FluidIngredientConverter());

        ItemListMod.setAdapter(new ReiItemListModAdapter());
    }

    @Override
    public String getPluginProviderName() {
        return "AE2";
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        registry.add(new TransformCategory());
        registry.add(new CondenserCategory());
        registry.add(new InscriberRecipeCategory());
        registry.add(new AttunementCategory());
        registry.add(new ChargerCategory());
        registry.add(new EntropyRecipeCategory());

        registerWorkingStations(registry);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (AEConfig.instance().isEnableFacadeRecipesInRecipeViewer()) {
            registry.registerGlobalDisplayGenerator(new FacadeRegistryGenerator());
        }

        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        registry.registerRecipeFiller(InscriberRecipe.class, AERecipeTypes.INSCRIBER, InscriberRecipeDisplay::new);
        registry.registerRecipeFiller(ChargerRecipe.class, AERecipeTypes.CHARGER, ChargerDisplay::new);
        registry.registerRecipeFiller(TransformRecipe.class, AERecipeTypes.TRANSFORM, TransformRecipeWrapper::new);
        registry.registerRecipeFiller(EntropyRecipe.class, AERecipeTypes.ENTROPY, EntropyRecipeDisplay::new);
        registry.registerRecipeFiller(StorageCellUpgradeRecipe.class, RecipeType.CRAFTING,
                this::convertStorageCellUpgradeRecipe);

        registry.add(new CondenserOutputDisplay(CondenserOutput.MATTER_BALLS));
        registry.add(new CondenserOutputDisplay(CondenserOutput.SINGULARITY));

        registerDescriptions(registry);
    }

    private Display convertStorageCellUpgradeRecipe(RecipeHolder<StorageCellUpgradeRecipe> holder) {
        var recipe = holder.value();

        return new DefaultCustomShapelessDisplay(
                holder,
                List.of(EntryIngredients.of(recipe.getInputCell()), EntryIngredients.of(recipe.getInputComponent())),
                List.of(EntryIngredients.of(recipe.getResultCell()), EntryIngredients.of(recipe.getResultComponent())));
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        // Allow recipe transfer from JEI to crafting and pattern terminal
        registry.register(new EncodePatternTransferHandler<>(PatternEncodingTermMenu.class));
        registry.register(new UseCraftingRecipeTransfer<>(CraftingTermMenu.class));
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        registry.registerDraggableStackVisitor(new GhostIngredientHandler());
        registry.registerFocusedStack((screen, mouse) -> {
            if (screen instanceof AEBaseScreen<?> aeScreen) {
                var stack = aeScreen.getStackUnderMouse(mouse.x, mouse.y);
                if (stack != null) {
                    for (var converter : IngredientConverters.getConverters()) {
                        var entryStack = converter.getIngredientFromStack(stack.stack());
                        if (entryStack != null) {
                            return CompoundEventResult.interruptTrue(entryStack);
                        }
                    }
                }
            }

            return CompoundEventResult.pass();
        });
        registry.registerContainerClickArea(
                new Rectangle(82, 39, 26, 16),
                InscriberScreen.class,
                InscriberRecipeCategory.ID);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(this::shouldEntryBeHidden);

        if (AEConfig.instance().isEnableFacadesInRecipeViewer()) {
            registry.addEntries(
                    EntryIngredients.ofItemStacks(FacadeCreativeTab.getDisplayItems()));
        }
    }

    @Override
    public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
        if (AEConfig.instance().isEnableFacadesInRecipeViewer()) {
            FacadeItem facadeItem = AEItems.FACADE.get();
            registry.group(AppEng.makeId("facades"), Component.translatable("itemGroup.ae2.facades"),
                    stack -> stack.getType() == VanillaEntryTypes.ITEM && stack.<ItemStack>castValue().is(facadeItem));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        if (CompatLayerHelper.IS_LOADED) {
            return;
        }

        zones.register(AEBaseScreen.class, screen -> {
            return screen != null ? mapRects(screen.getExclusionZones()) : Collections.emptyList();
        });

    }

    private static List<Rectangle> mapRects(List<Rect2i> exclusionZones) {
        return exclusionZones.stream()
                .map(ez -> new Rectangle(ez.getX(), ez.getY(), ez.getWidth(), ez.getHeight()))
                .collect(Collectors.toList());
    }

    private void registerWorkingStations(CategoryRegistry registry) {
        var condenser = AEBlocks.CONDENSER.stack();
        registry.addWorkstations(CondenserCategory.ID, EntryStacks.of(condenser));

        var inscriber = AEBlocks.INSCRIBER.stack();
        registry.addWorkstations(InscriberRecipeCategory.ID, EntryStacks.of(inscriber));
        registry.setPlusButtonArea(InscriberRecipeCategory.ID, ButtonArea.defaultArea());

        var craftingTerminal = AEParts.CRAFTING_TERMINAL.stack();
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(craftingTerminal));

        var wirelessCraftingTerminal = chargeFully(AEItems.WIRELESS_CRAFTING_TERMINAL.stack());
        registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(wirelessCraftingTerminal));

        registry.addWorkstations(ChargerDisplay.ID, EntryStacks.of(AEBlocks.CHARGER.stack()));
        registry.addWorkstations(ChargerDisplay.ID, EntryStacks.of(AEBlocks.CRANK.stack()));

        var entropyManipulator = chargeFully(chargeFully(AEItems.ENTROPY_MANIPULATOR.stack()));
        registry.addWorkstations(EntropyRecipeCategory.ID, EntryStacks.of(entropyManipulator));
    }

    private static ItemStack chargeFully(ItemStack stack) {
        if (stack.getItem() instanceof AEBasePoweredItem poweredItem) {
            poweredItem.injectAEPower(stack, poweredItem.getAEMaxPower(stack), Actionable.MODULATE);
        }
        return stack;
    }

    private void registerDescriptions(DisplayRegistry registry) {
        var all = EntryRegistry.getInstance().getEntryStacks().collect(EntryIngredient.collector());

        for (var entry : P2PTunnelAttunementInternal.getApiTunnels()) {
            var inputs = List.of(all.filter(
                    stack -> stack.getValue() instanceof ItemStack s && entry.stackPredicate().test(s)));
            if (inputs.isEmpty()) {
                continue;
            }

            registry.add(new AttunementDisplay(
                    inputs,
                    List.of(EntryIngredient.of(EntryStacks.of(entry.tunnelType()))),
                    ItemModText.P2P_API_ATTUNEMENT.text(),
                    entry.description()));
        }

        for (var entry : P2PTunnelAttunementInternal.getTagTunnels().entrySet()) {
            var ingredient = Ingredient.of(entry.getKey());
            if (ingredient.isEmpty()) {
                continue;
            }

            registry.add(new AttunementDisplay(List.of(EntryIngredients.ofIngredient(ingredient)),
                    List.of(EntryIngredient.of(EntryStacks.of(entry.getValue()))),
                    ItemModText.P2P_TAG_ATTUNEMENT.text()));
        }

        addDescription(registry, AEItems.CERTUS_QUARTZ_CRYSTAL, GuiText.CertusQuartzObtain.getTranslationKey());

        if (AEConfig.instance().isSpawnPressesInMeteoritesEnabled()) {
            addDescription(registry, AEItems.LOGIC_PROCESSOR_PRESS, GuiText.inWorldCraftingPresses.getTranslationKey());
            addDescription(registry, AEItems.CALCULATION_PROCESSOR_PRESS,
                    GuiText.inWorldCraftingPresses.getTranslationKey());
            addDescription(registry, AEItems.ENGINEERING_PROCESSOR_PRESS,
                    GuiText.inWorldCraftingPresses.getTranslationKey());
            addDescription(registry, AEItems.SILICON_PRESS, GuiText.inWorldCraftingPresses.getTranslationKey());
        }

        addDescription(registry, AEBlocks.CRANK.item(), ItemModText.CRANK_DESCRIPTION.getTranslationKey());
    }

    private static void addDescription(DisplayRegistry registry, ItemDefinition<?> itemDefinition, String... message) {
        DefaultInformationDisplay info = DefaultInformationDisplay.createFromEntry(EntryStacks.of(itemDefinition),
                itemDefinition.get().getDescription());
        info.lines(Arrays.stream(message).map(Component::translatable).collect(Collectors.toList()));
        registry.add(info);
    }

    private boolean shouldEntryBeHidden(EntryStack<?> entryStack) {
        if (entryStack.getType() != VanillaEntryTypes.ITEM) {
            return false;
        }
        return ItemPredicates.shouldBeHidden(entryStack.castValue());
    }

}
