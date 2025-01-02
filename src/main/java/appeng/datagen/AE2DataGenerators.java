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

package appeng.datagen;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import appeng.datagen.providers.loot.AE2LootTableProvider;
import appeng.datagen.providers.recipes.*;
import appeng.datagen.providers.tags.*;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import appeng.core.definitions.AEDamageTypes;
import appeng.datagen.providers.advancements.AdvancementGenerator;
import appeng.datagen.providers.localization.LocalizationProvider;
import appeng.datagen.providers.models.BlockModelProvider;
import appeng.datagen.providers.models.CableModelProvider;
import appeng.datagen.providers.models.DecorationModelProvider;
import appeng.datagen.providers.models.ItemModelProvider;
import appeng.datagen.providers.models.PartModelProvider;
import appeng.init.worldgen.InitBiomes;
import appeng.init.worldgen.InitDimensionTypes;
import appeng.init.worldgen.InitStructures;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

public class AE2DataGenerators {

    public static void onGatherData(
        FabricDataGenerator generator,
        ExistingFileHelper existingFileHelper,
        DataGenerator.PackGenerator pack
    ) {
        var registries = generator.getRegistries();
        var localization = new LocalizationProvider(generator);

        pack.addProvider(output -> new DatapackBuiltinEntriesProvider(output, registries,
            createDatapackEntriesBuilder()));

        // Loot
        pack.addProvider(packOutput -> new AE2LootTableProvider(packOutput, registries));

        // Tags
        var blockTagsProvider = pack
            .addProvider(packOutput -> new BlockTagsProvider(packOutput, registries));
        pack.addProvider(
            packOutput -> new ItemTagsProvider(packOutput, registries, blockTagsProvider.contentsGetter()));
        pack.addProvider(packOutput -> new FluidTagsProvider(packOutput, registries));
        pack.addProvider(packOutput -> new BiomeTagsProvider(packOutput, registries));
        pack.addProvider(packOutput -> new PoiTypeTagsProvider(packOutput, registries));
        pack.addProvider(packOutput -> new DataComponentTypeTagProvider(packOutput, registries, localization));

        // Models
        pack.addProvider(packOutput -> new BlockModelProvider(packOutput, existingFileHelper));
        pack.addProvider(packOutput -> new DecorationModelProvider(packOutput, existingFileHelper));
        pack.addProvider(packOutput -> new ItemModelProvider(packOutput, existingFileHelper));
        pack.addProvider(packOutput -> new CableModelProvider(packOutput, existingFileHelper));
        pack.addProvider(packOutput -> new PartModelProvider(packOutput, existingFileHelper));

        // Misc
        pack.addProvider(packOutput -> new AdvancementProvider(packOutput, registries, List.of(new AdvancementGenerator(localization))));

        // Recipes
        pack.addProvider(bindRegistries(DecorationRecipes::new, registries));
        pack.addProvider(bindRegistries(DecorationBlockRecipes::new, registries));
        pack.addProvider(bindRegistries(MatterCannonAmmoProvider::new, registries));
        pack.addProvider(bindRegistries(EntropyRecipes::new, registries));
        pack.addProvider(bindRegistries(InscriberRecipes::new, registries));
        pack.addProvider(bindRegistries(SmeltingRecipes::new, registries));
        pack.addProvider(bindRegistries(CraftingRecipes::new, registries));
        pack.addProvider(bindRegistries(SmithingRecipes::new, registries));
        pack.addProvider(bindRegistries(TransformRecipes::new, registries));
        pack.addProvider(bindRegistries(ChargerRecipes::new, registries));
        pack.addProvider(bindRegistries(QuartzCuttingRecipesProvider::new, registries));
        pack.addProvider(bindRegistries(UpgradeRecipes::new, registries));

        // Must run last
        pack.addProvider(packOutput -> localization);
    }

    private static <T extends DataProvider> DataProvider.Factory<T> bindRegistries(
            BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> factory,
            CompletableFuture<HolderLookup.Provider> factories) {
        return packOutput -> factory.apply(packOutput, factories);
    }


    private static RegistrySetBuilder createDatapackEntriesBuilder() {
        return new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, InitDimensionTypes::init)
            .add(Registries.STRUCTURE, InitStructures::initDatagenStructures)
            .add(Registries.STRUCTURE_SET, InitStructures::initDatagenStructureSets)
            .add(Registries.BIOME, InitBiomes::init)
            .add(Registries.DAMAGE_TYPE, AEDamageTypes::init);
    }
}
