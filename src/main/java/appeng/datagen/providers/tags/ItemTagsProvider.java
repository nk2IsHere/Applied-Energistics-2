/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.datagen.providers.tags;

import static net.minecraft.world.item.Items.*;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.ids.AETags;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.datagen.providers.IAE2DataProvider;

public class ItemTagsProvider extends FabricTagProvider.ItemTagProvider implements IAE2DataProvider {

    public ItemTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture,
            @Nullable BlockTagProvider blockTagProvider) {
        super(output, completableFuture, blockTagProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        copyBlockTags();

        // Allow the annihilation plane to be enchanted with silk touch, fortune, efficiency & unbreaking
        getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE).add(AEParts.ANNIHILATION_PLANE.asItem());
        getOrCreateTagBuilder(ItemTags.MINING_ENCHANTABLE).add(AEParts.ANNIHILATION_PLANE.asItem());
        getOrCreateTagBuilder(ItemTags.MINING_LOOT_ENCHANTABLE).add(AEParts.ANNIHILATION_PLANE.asItem());

        // Provide empty blacklist tags
        getOrCreateTagBuilder(AETags.ANNIHILATION_PLANE_ITEM_BLACKLIST);

        getOrCreateTagBuilder(ConventionTags.BUDDING_BLOCKS)
                .add(AEBlocks.FLAWLESS_BUDDING_QUARTZ.asItem())
                .add(AEBlocks.FLAWED_BUDDING_QUARTZ.asItem())
                .add(AEBlocks.CHIPPED_BUDDING_QUARTZ.asItem())
                .add(AEBlocks.DAMAGED_BUDDING_QUARTZ.asItem());
        getOrCreateTagBuilder(ConventionTags.BUDS)
                .add(AEBlocks.SMALL_QUARTZ_BUD.asItem())
                .add(AEBlocks.MEDIUM_QUARTZ_BUD.asItem())
                .add(AEBlocks.LARGE_QUARTZ_BUD.asItem());
        getOrCreateTagBuilder(ConventionTags.CLUSTERS)
                .add(AEBlocks.QUARTZ_CLUSTER.asItem());

        getOrCreateTagBuilder(ConventionTags.CERTUS_QUARTZ_DUST)
                .add(AEItems.CERTUS_QUARTZ_DUST.asItem());
        getOrCreateTagBuilder(ConventionTags.ENDER_PEARL_DUST)
                .add(AEItems.ENDER_DUST.asItem());
        getOrCreateTagBuilder(ConventionTags.SKY_STONE_DUST)
                .add(AEItems.SKY_DUST.asItem());

        getOrCreateTagBuilder(ConventionTags.ALL_QUARTZ_DUST)
                .addTag(ConventionTags.CERTUS_QUARTZ_DUST);

        getOrCreateTagBuilder(ConventionTags.ALL_CERTUS_QUARTZ)
                .addTag(ConventionTags.CERTUS_QUARTZ)
                .add(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem());
        getOrCreateTagBuilder(ConventionTags.ALL_FLUIX)
                .add(AEItems.FLUIX_CRYSTAL.asItem());
        getOrCreateTagBuilder(ConventionTags.ALL_NETHER_QUARTZ)
                .addOptionalTag(ConventionTags.NETHER_QUARTZ);
        getOrCreateTagBuilder(ConventionTags.ALL_QUARTZ)
                .addOptionalTag(ConventionTags.NETHER_QUARTZ)
                .addTag(ConventionTags.CERTUS_QUARTZ)
                .add(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem());

        for (AEColor color : AEColor.values()) {
            getOrCreateTagBuilder(ConventionTags.SMART_DENSE_CABLE).add(AEParts.SMART_DENSE_CABLE.item(color));
            getOrCreateTagBuilder(ConventionTags.SMART_CABLE).add(AEParts.SMART_CABLE.item(color));
            getOrCreateTagBuilder(ConventionTags.GLASS_CABLE).add(AEParts.GLASS_CABLE.item(color));
            getOrCreateTagBuilder(ConventionTags.COVERED_CABLE).add(AEParts.COVERED_CABLE.item(color));
            getOrCreateTagBuilder(ConventionTags.COVERED_DENSE_CABLE).add(AEParts.COVERED_DENSE_CABLE.item(color));
        }

        getOrCreateTagBuilder(ConventionTags.INSCRIBER_PRESSES)
                .add(AEItems.CALCULATION_PROCESSOR_PRESS.asItem())
                .add(AEItems.ENGINEERING_PROCESSOR_PRESS.asItem())
                .add(AEItems.LOGIC_PROCESSOR_PRESS.asItem())
                .add(AEItems.SILICON_PRESS.asItem());

        for (AEColor color : AEColor.VALID_COLORS) {
            getOrCreateTagBuilder(ConventionTags.PAINT_BALLS).add(AEItems.COLORED_PAINT_BALL.item(color));
            getOrCreateTagBuilder(ConventionTags.LUMEN_PAINT_BALLS).add(AEItems.COLORED_LUMEN_PAINT_BALL.item(color));
        }

        getOrCreateTagBuilder(ConventionTags.SILICON)
                .add(AEItems.SILICON.asItem());

        getOrCreateTagBuilder(ConventionTags.QUARTZ_AXE)
                .add(AEItems.CERTUS_QUARTZ_AXE.asItem())
                .add(AEItems.NETHER_QUARTZ_AXE.asItem());
        getOrCreateTagBuilder(ConventionTags.QUARTZ_HOE)
                .add(AEItems.CERTUS_QUARTZ_HOE.asItem())
                .add(AEItems.NETHER_QUARTZ_HOE.asItem());
        getOrCreateTagBuilder(ConventionTags.QUARTZ_PICK)
                .add(AEItems.CERTUS_QUARTZ_PICK.asItem())
                .add(AEItems.NETHER_QUARTZ_PICK.asItem());
        getOrCreateTagBuilder(ConventionTags.QUARTZ_SHOVEL)
                .add(AEItems.CERTUS_QUARTZ_SHOVEL.asItem())
                .add(AEItems.NETHER_QUARTZ_SHOVEL.asItem());
        getOrCreateTagBuilder(ConventionTags.QUARTZ_SWORD)
                .add(AEItems.CERTUS_QUARTZ_SWORD.asItem())
                .add(AEItems.NETHER_QUARTZ_SWORD.asItem());
        getOrCreateTagBuilder(ConventionTags.QUARTZ_WRENCH)
                .add(AEItems.CERTUS_QUARTZ_WRENCH.asItem())
                .add(AEItems.NETHER_QUARTZ_WRENCH.asItem());
        getOrCreateTagBuilder(ConventionTags.QUARTZ_KNIFE)
                .add(AEItems.CERTUS_QUARTZ_KNIFE.asItem())
                .add(AEItems.NETHER_QUARTZ_KNIFE.asItem());

        getOrCreateTagBuilder(ItemTags.AXES)
                .add(AEItems.CERTUS_QUARTZ_AXE.asItem())
                .add(AEItems.NETHER_QUARTZ_AXE.asItem())
                .add(AEItems.FLUIX_AXE.asItem());
        getOrCreateTagBuilder(ItemTags.HOES)
                .add(AEItems.CERTUS_QUARTZ_HOE.asItem())
                .add(AEItems.NETHER_QUARTZ_HOE.asItem())
                .add(AEItems.FLUIX_HOE.asItem());
        getOrCreateTagBuilder(ItemTags.PICKAXES)
                .add(AEItems.CERTUS_QUARTZ_PICK.asItem())
                .add(AEItems.NETHER_QUARTZ_PICK.asItem())
                .add(AEItems.FLUIX_PICK.asItem());
        getOrCreateTagBuilder(ItemTags.SHOVELS)
                .add(AEItems.CERTUS_QUARTZ_SHOVEL.asItem())
                .add(AEItems.NETHER_QUARTZ_SHOVEL.asItem())
                .add(AEItems.FLUIX_SHOVEL.asItem());
        getOrCreateTagBuilder(ItemTags.SWORDS)
                .add(AEItems.CERTUS_QUARTZ_SWORD.asItem())
                .add(AEItems.NETHER_QUARTZ_SWORD.asItem())
                .add(AEItems.FLUIX_SWORD.asItem());

        getOrCreateTagBuilder(ConventionTags.WRENCH).add(
                AEItems.CERTUS_QUARTZ_WRENCH.asItem(),
                AEItems.NETHER_QUARTZ_WRENCH.asItem(),
                AEItems.NETWORK_TOOL.asItem());

        getOrCreateTagBuilder(AETags.METAL_INGOTS)
                .addOptionalTag(ResourceLocation.parse("c:ingots/copper"))
                .addOptionalTag(ResourceLocation.parse("c:ingots/tin"))
                .addOptionalTag(ResourceLocation.parse("c:ingots/iron"))
                .addOptionalTag(ResourceLocation.parse("c:ingots/gold"))
                .addOptionalTag(ResourceLocation.parse("c:ingots/brass"))
                .addOptionalTag(ResourceLocation.parse("c:ingots/nickel"))
                .addOptionalTag(ResourceLocation.parse("c:ingots/aluminium"));

        getOrCreateTagBuilder(ConventionTags.PATTERN_PROVIDER)
                .add(AEParts.PATTERN_PROVIDER.asItem())
                .add(AEBlocks.PATTERN_PROVIDER.asItem());

        getOrCreateTagBuilder(ConventionTags.INTERFACE)
                .add(AEParts.INTERFACE.asItem())
                .add(AEBlocks.INTERFACE.asItem());

        getOrCreateTagBuilder(ConventionTags.ILLUMINATED_PANEL)
                .add(AEParts.MONITOR.asItem())
                .add(AEParts.SEMI_DARK_MONITOR.asItem())
                .add(AEParts.DARK_MONITOR.asItem());

        getOrCreateTagBuilder(ConventionTags.FLUIX_DUST)
                .add(AEItems.FLUIX_DUST.asItem());
        getOrCreateTagBuilder(ConventionTags.CERTUS_QUARTZ_DUST)
                .add(AEItems.CERTUS_QUARTZ_DUST.asItem());

        getOrCreateTagBuilder(ConventionTags.FLUIX_CRYSTAL)
                .add(AEItems.FLUIX_CRYSTAL.asItem());
        getOrCreateTagBuilder(ConventionTags.CERTUS_QUARTZ)
                .add(AEItems.CERTUS_QUARTZ_CRYSTAL.asItem())
                .add(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem());

        getOrCreateTagBuilder(ConventionTags.DUSTS)
                .add(AEItems.CERTUS_QUARTZ_DUST.asItem())
                .add(AEItems.ENDER_DUST.asItem())
                .add(AEItems.FLUIX_DUST.asItem())
                .add(AEItems.SKY_DUST.asItem());

        getOrCreateTagBuilder(ConventionTags.GEMS)
                .add(AEItems.CERTUS_QUARTZ_CRYSTAL.asItem())
                .add(AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem())
                .add(AEItems.FLUIX_CRYSTAL.asItem());

        // Fabric replacement for ToolActions for now
        getOrCreateTagBuilder(ConventionTags.WRENCH).add(
                AEItems.CERTUS_QUARTZ_WRENCH.asItem(),
                AEItems.NETHER_QUARTZ_WRENCH.asItem(),
                AEItems.NETWORK_TOOL.asItem());

        getOrCreateTagBuilder(ConventionTags.CURIOS).add(
                AEItems.WIRELESS_TERMINAL.asItem(),
                AEItems.WIRELESS_CRAFTING_TERMINAL.asItem(),
                AEItems.PORTABLE_ITEM_CELL1K.asItem(),
                AEItems.PORTABLE_ITEM_CELL4K.asItem(),
                AEItems.PORTABLE_ITEM_CELL16K.asItem(),
                AEItems.PORTABLE_ITEM_CELL64K.asItem(),
                AEItems.PORTABLE_ITEM_CELL256K.asItem(),
                AEItems.PORTABLE_FLUID_CELL1K.asItem(),
                AEItems.PORTABLE_FLUID_CELL4K.asItem(),
                AEItems.PORTABLE_FLUID_CELL16K.asItem(),
                AEItems.PORTABLE_FLUID_CELL64K.asItem(),
                AEItems.PORTABLE_FLUID_CELL256K.asItem());

        getOrCreateTagBuilder(ConventionTags.CAN_REMOVE_COLOR).add(WATER_BUCKET, SNOWBALL);

        // Manually add tags for mods that are unlikely to do it themselves since we don't want to force users to craft
        getOrCreateTagBuilder(ConventionTags.WRENCH).addOptional(ResourceLocation.parse("immersiveengineering:hammer"));

        addP2pAttunementTags();
    }

    // Copy the entries AE2 added to certain block tags over to asItem tags of the same name
    // Assumes that asItems or asItem tags generally have the same name as the block equivalent.
    private void copyBlockTags() {
        mirrorBlockTag(ConventionalBlockTags.STORAGE_BLOCKS.location());
        mirrorBlockTag(ResourceLocation.parse("c:storage_blocks/certus_quartz"));
        copy(BlockTags.WALLS, ItemTags.WALLS);
        copy(ConventionalBlockTags.CHESTS, ConventionalItemTags.CHESTS);
        copy(ConventionTags.GLASS_BLOCK, ConventionTags.GLASS);
    }

    private void mirrorBlockTag(ResourceLocation tagName) {
        copy(TagKey.create(Registries.BLOCK, tagName), TagKey.create(Registries.ITEM, tagName));
    }

    private void addP2pAttunementTags() {
        getOrCreateTagBuilder(P2PTunnelAttunement.getAttunementTag(P2PTunnelAttunement.LIGHT_TUNNEL))
                .add(TORCH, GLOWSTONE);

        getOrCreateTagBuilder(P2PTunnelAttunement.getAttunementTag(P2PTunnelAttunement.ENERGY_TUNNEL))
                .add(AEBlocks.DENSE_ENERGY_CELL.asItem(), AEBlocks.ENERGY_ACCEPTOR.asItem(),
                        AEBlocks.ENERGY_CELL.asItem(), AEBlocks.CREATIVE_ENERGY_CELL.asItem());

        getOrCreateTagBuilder(P2PTunnelAttunement.getAttunementTag(P2PTunnelAttunement.REDSTONE_TUNNEL))
                .add(REDSTONE, REPEATER, REDSTONE_LAMP, COMPARATOR, DAYLIGHT_DETECTOR,
                        REDSTONE_TORCH, REDSTONE_BLOCK, LEVER);

        getOrCreateTagBuilder(P2PTunnelAttunement.getAttunementTag(P2PTunnelAttunement.ITEM_TUNNEL))
                .add(AEParts.STORAGE_BUS.asItem(), AEParts.EXPORT_BUS.asItem(), AEParts.IMPORT_BUS.asItem())
                .add(HOPPER, CHEST, TRAPPED_CHEST)
                .addTag(ConventionTags.INTERFACE);

        getOrCreateTagBuilder(P2PTunnelAttunement.getAttunementTag(P2PTunnelAttunement.FLUID_TUNNEL))
                .add(BUCKET, MILK_BUCKET, WATER_BUCKET, LAVA_BUCKET);

        getOrCreateTagBuilder(P2PTunnelAttunement.getAttunementTag(P2PTunnelAttunement.ME_TUNNEL))
                .addTag(ConventionTags.COVERED_CABLE)
                .addTag(ConventionTags.COVERED_DENSE_CABLE)
                .addTag(ConventionTags.GLASS_CABLE)
                .addTag(ConventionTags.SMART_CABLE)
                .addTag(ConventionTags.SMART_DENSE_CABLE);
    }
}
