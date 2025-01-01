package appeng.client.gui.style;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class TooltipArea extends Position {
    private int width;
    private int height;

    private List<Component> tooltip = new ArrayList<>();

    public List<Component> getTooltip() {
        return tooltip;
    }

    public void setTooltip(List<Component> tooltip) {
        this.tooltip = tooltip;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
