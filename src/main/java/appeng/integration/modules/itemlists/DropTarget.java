package appeng.integration.modules.itemlists;

import appeng.api.stacks.GenericStack;
import net.minecraft.client.renderer.Rect2i;

public interface DropTarget {
    Rect2i area();

    boolean canDrop(GenericStack stack);

    boolean drop(GenericStack stack);
}
