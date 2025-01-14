/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.widgets;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import appeng.client.gui.Icon;

public class TabButton extends Button implements ITooltip {
    private Style style = Style.BOX;
    private Icon icon = null;
    private ItemStack item;

    private boolean selected;

    private boolean disableBackground = false;

    public enum Style {
        CORNER,
        BOX,
        HORIZONTAL
    }

    public TabButton(Icon ico, Component message, OnPress onPress) {
        super(0, 0, 22, 22, message, onPress, Button.DEFAULT_NARRATION);

        this.icon = ico;
    }

    /**
     * Using itemstack as an icon
     *
     * @param ico     used icon
     * @param message mouse over message
     */
    public TabButton(ItemStack ico, Component message, OnPress onPress) {
        super(0, 0, 22, 22, message, onPress, Button.DEFAULT_NARRATION);
        this.item = ico;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int x, int y, float partial) {
        if (this.visible) {
            // Selects the button border from the sprite-sheet, where each type occupies a
            // 2x2 slot
            var backdrop = switch (this.style) {
                case CORNER -> this.isFocused() ? Icon.TAB_BUTTON_BACKGROUND_BORDERLESS_FOCUS
                        : Icon.TAB_BUTTON_BACKGROUND_BORDERLESS;
                case BOX -> this.isFocused() ? Icon.TAB_BUTTON_BACKGROUND_FOCUS : Icon.TAB_BUTTON_BACKGROUND;
                case HORIZONTAL -> {
                    if (this.isFocused()) {
                        yield Icon.HORIZONTAL_TAB_FOCUS;
                    } else if (this.selected) {
                        yield Icon.HORIZONTAL_TAB_SELECTED;
                    }
                    yield Icon.HORIZONTAL_TAB;
                }
            };
            if (!disableBackground) {
                backdrop.getBlitter().dest(getX(), getY()).blit(guiGraphics);
            }

            var iconX = switch (this.style) {
                case CORNER -> 1;
                case BOX -> 2;
                case HORIZONTAL -> 3;
            };
            var iconY = switch (this.style) {
                case CORNER -> 1;
                case BOX -> 2;
                case HORIZONTAL -> 3;
            };

            if (this.icon != null) {
                this.icon.getBlitter().dest(getX() + iconX, getY() + iconY - 1).blit(guiGraphics);
            }

            if (this.item != null) {
                var pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(0f, -1f, 100);
                guiGraphics.renderItem(this.item, getX() + iconX, getY() + iconY);
                var font = Minecraft.getInstance().font;
                guiGraphics.renderItemDecorations(font, this.item, getX() + iconX, getY() + iconY);
                pose.popPose();
            }
        }
    }

    @Override
    public List<Component> getTooltipMessage() {
        return Collections.singletonList(getMessage());
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), width, height);
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return this.visible;
    }

    public Style getStyle() {
        return this.style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDisableBackground() {
        return disableBackground;
    }

    public void setDisableBackground(boolean disableBackground) {
        this.disableBackground = disableBackground;
    }
}
