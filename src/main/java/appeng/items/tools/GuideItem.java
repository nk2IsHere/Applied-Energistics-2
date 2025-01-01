package appeng.items.tools;

import appeng.core.AppEng;
import appeng.items.AEBaseItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Shows the guidebook when used.
 */
public class GuideItem extends AEBaseItem {
    public GuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            openGuide();
        }

        return new InteractionResultHolder<>(InteractionResult.FAIL, stack);
    }

    private static void openGuide() {
        AppEng.instance().openGuideAtPreviousPage(AppEng.makeId("index.md"));
    }
}
