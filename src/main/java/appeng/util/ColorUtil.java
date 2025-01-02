package appeng.util;

import net.minecraft.client.color.item.ItemColor;

public class ColorUtil {

    public static int toARGB(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int toARGB(int color) {
        return 0xFF000000 | color;
    }

    public static ItemColor toARGB(ItemColor itemColor) {
        return (stack, tintIndex) -> toARGB(itemColor.getColor(stack, tintIndex));
    }
}
