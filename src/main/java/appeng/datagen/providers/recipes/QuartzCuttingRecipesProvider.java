package appeng.datagen.providers.recipes;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.crafting.Ingredient;

import appeng.api.ids.AETags;
import appeng.core.AppEng;
import appeng.core.definitions.AEParts;
import appeng.datagen.providers.tags.ConventionTags;
import appeng.recipes.quartzcutting.QuartzCuttingRecipe;

public class QuartzCuttingRecipesProvider extends AE2RecipeProvider {

    public QuartzCuttingRecipesProvider(
            FabricDataOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(
                output,
                registriesFuture);
    }

    @Override
    public void buildRecipes(RecipeOutput recipeOutput) {
        recipeOutput.accept(
                AppEng.makeId("network/parts/cable_anchor"),
                new QuartzCuttingRecipe(
                        AEParts.CABLE_ANCHOR.stack(4),
                        NonNullList.of(Ingredient.EMPTY,
                                Ingredient.of(ConventionTags.QUARTZ_KNIFE),
                                Ingredient.of(AETags.METAL_INGOTS))),
                null);
    }

    @Override
    public String getName() {
        return "AE2 Quartz Cutting Recipes";
    }
}
