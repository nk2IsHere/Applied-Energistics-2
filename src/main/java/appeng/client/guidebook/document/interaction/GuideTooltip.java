package appeng.client.guidebook.document.interaction;

import appeng.siteexport.ExportableResourceProvider;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface GuideTooltip extends ExportableResourceProvider {

    default ItemStack getIcon() {
        return ItemStack.EMPTY;
    }

    List<ClientTooltipComponent> getLines();

}
