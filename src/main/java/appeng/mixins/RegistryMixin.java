package appeng.mixins;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.callback.RegistryCallbackHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public class RegistryMixin {

    @Shadow private boolean frozen;

    @Inject(method = "register*", at = @At("TAIL"))
    public <T> void register(
        ResourceKey<T> resourceKey,
        T object,
        RegistrationInfo registrationInfo,
        CallbackInfoReturnable<Holder.Reference<T>> cir
    ) {
        var registry = (Registry<T>) this;
        RegistryCallbackHolder.INSTANCE.onAdd(registry, resourceKey, object);
    }

    @Inject(method = "freeze", at = @At("TAIL"))
    public void freeze(CallbackInfoReturnable<Registry<?>> cir) {
        var registry = (Registry<?>) this;
        if(!this.frozen) {
            RegistryCallbackHolder.INSTANCE.onBake(registry);
        }
    }
}
