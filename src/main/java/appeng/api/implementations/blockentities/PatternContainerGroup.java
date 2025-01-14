package appeng.api.implementations.blockentities;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.core.localization.GuiText;

/**
 * Provides both a key for grouping pattern providers, and displaying the group in the pattern access terminal.
 * <p/>
 * The group can be the crafting container itself, for example if it is given a custom name by the player. It might
 * default to the crafting machine it is adjacent otherwise.
 *
 * @param icon    The icon to display when referring to the grouped containers. This stack will NEVER be modified. It
 *                will be used to show an icon, as well as compared to group similar containers. It may be
 *                {@link ItemStack#EMPTY}
 * @param name    The name to use to refer to the group.
 * @param tooltip Additional tooltip lines to describe the group (i.e. installed upgrades).
 */
public record PatternContainerGroup(
        @Nullable AEItemKey icon,
        Component name,
        List<Component> tooltip) {

    private static final PatternContainerGroup NOTHING = new PatternContainerGroup(AEItemKey.of(Items.AIR),
            GuiText.Nothing.text(), List.of());

    public static PatternContainerGroup nothing() {
        return NOTHING;
    }

    public void writeToPacket(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(icon != null);
        if (icon != null) {
            icon.writeToPacket(buffer);
        }
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, name);
        buffer.writeVarInt(tooltip.size());
        for (var component : tooltip) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buffer, component);
        }
    }

    public static PatternContainerGroup readFromPacket(RegistryFriendlyByteBuf buffer) {
        var icon = buffer.readBoolean() ? AEItemKey.fromPacket(buffer) : null;
        var name = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer);
        var lineCount = buffer.readVarInt();
        var lines = new ArrayList<Component>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            lines.add(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buffer));
        }
        return new PatternContainerGroup(icon, name, lines);
    }

    @Nullable
    public static PatternContainerGroup fromMachine(Level level, BlockPos pos, Direction side) {
        // Check for first-class support
        var craftingMachine = ICraftingMachine.of(level, pos, side);
        if (craftingMachine != null) {
            return craftingMachine.getCraftingMachineInfo();
        }

        // Anything else requires a block entity
        var target = level.getBlockEntity(pos);
        if (target == null) {
            return null;
        }

        // Heuristic: If it doesn't allow item or fluid transfers, ignore it
        // var adaptor = InternalInventory.wrapExternal(target, side);
        // if (adaptor == null || !adaptor.mayAllowInsertion()) {
        // return null;
        // }
        var itemStorage = ItemStorage.SIDED.find(target.getLevel(), pos, side);
        if (itemStorage == null) {
            return null;
        }

        var fluidStorage = FluidStorage.SIDED.find(target.getLevel(), pos, side);
        if (fluidStorage == null) {
            return null;
        }

        AEItemKey icon;
        Component name;
        List<Component> tooltip = List.of();

        // For scenarios like pattern providers against cable bus
        if (target instanceof IPartHost partHost) {
            var part = partHost.getPart(side);
            if (part == null) {
                // No part at side -> ignore, we don't interface with the cable
                return null;
            }

            icon = AEItemKey.of(part.getPartItem());
            if (part instanceof Nameable nameable) {
                name = nameable.getDisplayName();
            } else {
                name = icon.getDisplayName();
            }
        } else {
            // Try to wrestle an item from the adjacent block entity
            // TODO On Forge we can use pick block here
            var targetBlock = target.getBlockState().getBlock();
            var targetItem = new ItemStack(targetBlock);
            icon = AEItemKey.of(targetItem);

            if (target instanceof Nameable nameable && nameable.hasCustomName()) {
                name = nameable.getCustomName();
            } else {
                if (targetItem.isEmpty()) {
                    // If the object isn't named, and we didn't get an item from it,
                    // we can't show either icon nor name!
                    return null;
                }
                name = targetItem.getHoverName();
            }
        }

        return new PatternContainerGroup(icon, name, tooltip);
    }
}
