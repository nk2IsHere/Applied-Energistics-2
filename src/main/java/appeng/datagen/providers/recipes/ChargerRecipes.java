package appeng.datagen.providers.recipes;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.recipes.handlers.ChargerRecipeBuilder;

public class ChargerRecipes extends AE2RecipeProvider {

    public ChargerRecipes(
            FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(
                output,
                registriesFuture);
    }

    @Override
    public void buildRecipes(RecipeOutput consumer) {
        ChargerRecipeBuilder.charge(consumer,
                AppEng.makeId("charger/charged_certus_quartz_crystal"),
                AEItems.CERTUS_QUARTZ_CRYSTAL, AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED);
        ChargerRecipeBuilder.charge(consumer,
                AppEng.makeId("charger/meteorite_compass"),
                Items.COMPASS, AEItems.METEORITE_COMPASS);
        ChargerRecipeBuilder.charge(consumer,
                AppEng.makeId("charger/guide"),
                Items.BOOK, AEItems.TABLET);
    }

    @Override
    public String getName() {
        return "AE2 Charger Recipes";
    }
}
