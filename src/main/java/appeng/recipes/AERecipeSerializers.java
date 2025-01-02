package appeng.recipes;

import appeng.core.AppEng;
import appeng.recipes.entropy.EntropyRecipeSerializer;
import appeng.recipes.game.*;
import appeng.recipes.handlers.ChargerRecipeSerializer;
import appeng.recipes.handlers.InscriberRecipeSerializer;
import appeng.recipes.mattercannon.MatterCannonAmmoSerializer;
import appeng.recipes.quartzcutting.QuartzCuttingRecipeSerializer;
import appeng.recipes.transform.TransformRecipeSerializer;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class AERecipeSerializers {
    private AERecipeSerializers() {
    }

    public static final DeferredRegister<RecipeSerializer<?>> DR = DeferredRegister
            .create(AppEng.MOD_ID, Registries.RECIPE_SERIALIZER);

    static {
        register("inscriber", InscriberRecipeSerializer.INSTANCE);
        register("facade", FacadeRecipe.SERIALIZER);
        register("entropy", EntropyRecipeSerializer.INSTANCE);
        register("matter_cannon", MatterCannonAmmoSerializer.INSTANCE);
        register("transform", TransformRecipeSerializer.INSTANCE);
        register("charger", ChargerRecipeSerializer.INSTANCE);
        register("storage_cell_upgrade", StorageCellUpgradeRecipeSerializer.INSTANCE);
        register("add_item_upgrade", AddItemUpgradeRecipeSerializer.INSTANCE);
        register("remove_item_upgrade", RemoveItemUpgradeRecipeSerializer.INSTANCE);
        register("quartz_cutting", QuartzCuttingRecipeSerializer.INSTANCE);
        register("crafting_unit_transform", CraftingUnitTransformRecipeSerializer.INSTANCE);
        register("storage_cell_disassembly", StorageCellDisassemblyRecipeSerializer.INSTANCE);
    }

    private static void register(String id, RecipeSerializer<?> serializer) {
        DR.register(id, () -> serializer);
    }
}
