package appeng.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MouseHandler;

import appeng.hooks.MouseWheelScrolled;

/**
 * Emulates the Forge MouseWheel-Event that is triggered outside of UIs
 */
@Mixin(MouseHandler.class)
public class MouseWheelMixin {

    /**
     * Inject right before the slot-cycling that would normally be caused by the scroll-wheel
     */
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;swapPaint(D)V"), cancellable = true)
    public void onScrollWithoutScreen(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (MouseWheelScrolled.EVENT.invoker().onWheelScrolled(xOffset, yOffset)) {
            ci.cancel();
        }
    }
}
