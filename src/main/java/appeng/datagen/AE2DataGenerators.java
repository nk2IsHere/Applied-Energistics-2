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

import appeng.datagen.providers.loot.BlockDropProvider;
import appeng.datagen.providers.recipes.*;
import appeng.datagen.providers.tags.*;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
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

public class AE2DataGenerators {

    public static void onGatherData(
        FabricDataGenerator generator,
        ExistingFileHelper existingFileHelper
    ) {
        var pack = generator.createPack();
        var localization = new LocalizationProvider(generator);

//        pack.addProvider(output -> new DatapackBuiltinEntriesProvider(output, registries,
//            createDatapackEntriesBuilder()));

        // Loot
        pack.addProvider(BlockDropProvider::new);

        // Tags
        var blockTagsProvider = pack.addProvider(BlockTagsProvider::new);
        pack.addProvider((packOutput, registries) -> new ItemTagsProvider(packOutput, registries, blockTagsProvider));
        pack.addProvider(FluidTagsProvider::new);
        pack.addProvider(BiomeTagsProvider::new);
        pack.addProvider(PoiTypeTagsProvider::new);
        pack.addProvider((packOutput, registries) -> new DataComponentTypeTagProvider(packOutput, registries, localization));

        // Models
        pack.addProvider((packOutput, registries) -> new BlockModelProvider(packOutput, existingFileHelper));
        pack.addProvider((packOutput, registries) -> new DecorationModelProvider(packOutput, existingFileHelper));
        pack.addProvider((packOutput, registries) -> new ItemModelProvider(packOutput, existingFileHelper));
        pack.addProvider((packOutput, registries) -> new CableModelProvider(packOutput, existingFileHelper));
        pack.addProvider((packOutput, registries) -> new PartModelProvider(packOutput, existingFileHelper));

        // Misc
        pack.addProvider((packOutput, registries) -> new AdvancementGenerator(packOutput, registries, localization));

        // Recipes
        pack.addProvider(DecorationRecipes::new);
        pack.addProvider(DecorationBlockRecipes::new);
        pack.addProvider(MatterCannonAmmoProvider::new);
        pack.addProvider(EntropyRecipes::new);
        pack.addProvider(InscriberRecipes::new);
        pack.addProvider(SmeltingRecipes::new);
        pack.addProvider(CraftingRecipes::new);
        pack.addProvider(SmithingRecipes::new);
        pack.addProvider(TransformRecipes::new);
        pack.addProvider(ChargerRecipes::new);
        pack.addProvider(QuartzCuttingRecipesProvider::new);
        pack.addProvider(UpgradeRecipes::new);

        // Must run last
        pack.addProvider((packOutput, registries) -> localization);
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
