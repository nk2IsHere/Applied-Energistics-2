package appeng.integration.modules.jade;

import appeng.api.integrations.igtooltip.TooltipContext;
import snownee.jade.api.BlockAccessor;

class ContextHelper {
    private ContextHelper() {
    }

    public static TooltipContext getContext(BlockAccessor accessor) {
        return new TooltipContext(
                accessor.getServerData(),
                accessor.getHitResult().getLocation(),
                accessor.getPlayer());
    }
}
