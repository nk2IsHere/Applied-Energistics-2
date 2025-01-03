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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import appeng.api.ids.AETags;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.BlockDefinition;
import appeng.datagen.providers.IAE2DataProvider;

public class BlockTagsProvider extends FabricTagProvider.BlockTagProvider implements IAE2DataProvider {
    public BlockTagsProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Black- and whitelist tags
        getOrCreateTagBuilder(AETags.SPATIAL_BLACKLIST)
                .add(Blocks.BEDROCK)
                .addOptionalTag(ConventionTags.IMMOVABLE_BLOCKS.location());
        getOrCreateTagBuilder(AETags.ANNIHILATION_PLANE_BLOCK_BLACKLIST);
        getOrCreateTagBuilder(AETags.FACADE_BLOCK_WHITELIST)
                .add(AEBlocks.QUARTZ_GLASS.block(), AEBlocks.QUARTZ_VIBRANT_GLASS.block(),
                        Blocks.CHISELED_BOOKSHELF, Blocks.JUKEBOX, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.DROPPER,
                        Blocks.DISPENSER, Blocks.CRAFTER, Blocks.BARREL, Blocks.BEE_NEST, Blocks.BEEHIVE,
                        Blocks.SCULK_CATALYST, Blocks.SOUL_SAND, Blocks.HONEY_BLOCK,
                        AEBlocks.CONTROLLER.block(), AEBlocks.CRAFTING_STORAGE_1K.block(),
                        AEBlocks.CRAFTING_STORAGE_4K.block(), AEBlocks.CRAFTING_STORAGE_16K.block(),
                        AEBlocks.CRAFTING_STORAGE_64K.block(), AEBlocks.CRAFTING_STORAGE_256K.block(),
                        AEBlocks.CRAFTING_MONITOR.block(), AEBlocks.CRAFTING_UNIT.block(),
                        AEBlocks.CRAFTING_ACCELERATOR.block())
                .addOptionalTag(ConventionTags.GLASS_BLOCK.location());
        getOrCreateTagBuilder(AETags.GROWTH_ACCELERATABLE)
                // TODO: Should all be in some conventional tag
                .add(Blocks.BAMBOO_SAPLING, Blocks.BAMBOO, Blocks.SUGAR_CANE, Blocks.SUGAR_CANE, Blocks.VINE,
                        Blocks.TWISTING_VINES, Blocks.WEEPING_VINES, Blocks.CAVE_VINES, Blocks.SWEET_BERRY_BUSH,
                        Blocks.NETHER_WART, Blocks.KELP, Blocks.COCOA)
                .addOptionalTag(ConventionTags.CROPS.location())
                .addOptionalTag(ConventionTags.SAPLINGS.location())
                .addTag(ConventionTags.BUDDING_BLOCKS_BLOCKS);

        getOrCreateTagBuilder(ConventionTags.BUDDING_BLOCKS_BLOCKS)
                .add(AEBlocks.FLAWLESS_BUDDING_QUARTZ.block())
                .add(AEBlocks.FLAWED_BUDDING_QUARTZ.block())
                .add(AEBlocks.CHIPPED_BUDDING_QUARTZ.block())
                .add(AEBlocks.DAMAGED_BUDDING_QUARTZ.block());
        getOrCreateTagBuilder(ConventionTags.BUDS_BLOCKS)
                .add(AEBlocks.SMALL_QUARTZ_BUD.block())
                .add(AEBlocks.MEDIUM_QUARTZ_BUD.block())
                .add(AEBlocks.LARGE_QUARTZ_BUD.block());
        getOrCreateTagBuilder(ConventionTags.CLUSTERS_BLOCKS)
                .add(AEBlocks.QUARTZ_CLUSTER.block());

        getOrCreateTagBuilder(ConventionTags.CERTUS_QUARTZ_STORAGE_BLOCK_BLOCK)
                .add(AEBlocks.QUARTZ_BLOCK.block());
        getOrCreateTagBuilder(ConventionalBlockTags.STORAGE_BLOCKS)
                .addTag(ConventionTags.CERTUS_QUARTZ_STORAGE_BLOCK_BLOCK);

        // Special behavior is associated with this tag, so our walls need to be added to it
        getOrCreateTagBuilder(BlockTags.WALLS).add(
                AEBlocks.SKY_STONE_WALL.block(),
                AEBlocks.SMOOTH_SKY_STONE_WALL.block(),
                AEBlocks.SKY_STONE_BRICK_WALL.block(),
                AEBlocks.SKY_STONE_SMALL_BRICK_WALL.block(),
                AEBlocks.FLUIX_WALL.block(),
                AEBlocks.QUARTZ_WALL.block(),
                AEBlocks.CUT_QUARTZ_WALL.block(),
                AEBlocks.SMOOTH_QUARTZ_WALL.block(),
                AEBlocks.QUARTZ_BRICK_WALL.block(),
                AEBlocks.CHISELED_QUARTZ_WALL.block(),
                AEBlocks.QUARTZ_PILLAR_WALL.block());

        getOrCreateTagBuilder(ConventionalBlockTags.CHESTS).add(AEBlocks.SKY_STONE_CHEST.block(),
                AEBlocks.SMOOTH_SKY_STONE_CHEST.block());
        getOrCreateTagBuilder(ConventionTags.GLASS_BLOCK).add(AEBlocks.QUARTZ_GLASS.block(),
                AEBlocks.QUARTZ_VIBRANT_GLASS.block());

        // Fixtures should cause walls to have posts
        getOrCreateTagBuilder(BlockTags.WALL_POST_OVERRIDE).add(AEBlocks.QUARTZ_FIXTURE.block(),
                AEBlocks.LIGHT_DETECTOR.block());

        addEffectiveTools();
    }

    /**
     * All sky-stone related blocks should be minable with iron-pickaxes and up.
     */
    private static final BlockDefinition<?>[] SKY_STONE_BLOCKS = {
            AEBlocks.SKY_STONE_BLOCK,
            AEBlocks.SMOOTH_SKY_STONE_BLOCK,
            AEBlocks.SKY_STONE_BRICK,
            AEBlocks.SKY_STONE_SMALL_BRICK,
            AEBlocks.SKY_STONE_CHEST,
            AEBlocks.SMOOTH_SKY_STONE_CHEST,
            AEBlocks.SKY_STONE_STAIRS,
            AEBlocks.SMOOTH_SKY_STONE_STAIRS,
            AEBlocks.SKY_STONE_BRICK_STAIRS,
            AEBlocks.SKY_STONE_SMALL_BRICK_STAIRS,
            AEBlocks.SKY_STONE_WALL,
            AEBlocks.SMOOTH_SKY_STONE_WALL,
            AEBlocks.SKY_STONE_BRICK_WALL,
            AEBlocks.SKY_STONE_SMALL_BRICK_WALL,
            AEBlocks.SKY_STONE_SLAB,
            AEBlocks.SMOOTH_SKY_STONE_SLAB,
            AEBlocks.SKY_STONE_BRICK_SLAB,
            AEBlocks.SKY_STONE_SMALL_BRICK_SLAB
    };

    private void addEffectiveTools() {
        Map<BlockDefinition<?>, List<TagKey<Block>>> specialTags = new HashMap<>();
        for (var skyStoneBlock : SKY_STONE_BLOCKS) {
            specialTags.put(skyStoneBlock, List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL));
        }
        var defaultTags = List.of(BlockTags.MINEABLE_WITH_PICKAXE);

        for (var block : AEBlocks.getBlocks()) {
            for (var desiredTag : specialTags.getOrDefault(block, defaultTags)) {
                getOrCreateTagBuilder(desiredTag).add(block.block());
            }
        }

    }
}
