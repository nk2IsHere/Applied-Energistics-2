package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import appeng.hooks.IntrinsicEnchantItem;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
    @Inject(at = @At("RETURN"), method = "getItemEnchantmentLevel", cancellable = true)
    private static void hookGetItemEnchantmentLevel(Holder<Enchantment> holder, ItemStack itemStack,
            CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0 && itemStack.getItem() instanceof IntrinsicEnchantItem item) {
            int level = item.getIntrinsicEnchantLevel(itemStack, holder);
            if (level != 0) {
                cir.setReturnValue(level);
            }
        }
    }
}
