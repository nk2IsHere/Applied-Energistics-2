package appeng.api.integrations.igtooltip.providers;

import appeng.api.integrations.igtooltip.TooltipContext;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the name shown in the in-game tooltip.
 */
@ApiStatus.Experimental
@ApiStatus.OverrideOnly
@FunctionalInterface
public interface NameProvider<T> {
    /**
     * @return Null if this provider can't provide a name for the object.
     */
    @Nullable
    Component getName(T object, TooltipContext context);
}
