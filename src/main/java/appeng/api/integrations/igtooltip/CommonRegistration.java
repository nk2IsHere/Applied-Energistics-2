package appeng.api.integrations.igtooltip;

import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface CommonRegistration {
    <T extends BlockEntity> void addBlockEntityData(ResourceLocation id,
            Class<T> blockEntityClass,
            ServerDataProvider<? super T> provider);

}
