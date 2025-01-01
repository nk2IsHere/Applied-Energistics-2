package appeng.items.storage;

import appeng.api.stacks.GenericStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record StorageCellTooltipComponent(List<ItemStack> upgrades,
        List<GenericStack> content,
        boolean hasMoreContent,
        boolean showAmounts) implements TooltipComponent {
}
