package appeng.client.gui.me.common;

import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A slot to showcase an item on the client-side.
 */
public class ClientDisplaySlot extends ClientReadOnlySlot {
    private final ItemStack item;

    public ClientDisplaySlot(@Nullable GenericStack stack) {
        item = GenericStack.wrapInItemStack(stack);
    }

    @Override
    public ItemStack getItem() {
        return item;
    }
}
