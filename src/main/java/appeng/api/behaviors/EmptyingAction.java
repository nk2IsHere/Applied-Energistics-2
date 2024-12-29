package appeng.api.behaviors;

import appeng.api.stacks.AEKey;
import net.minecraft.network.chat.Component;

/**
 * Describes the action of emptying an item into the storage network.
 */
public record EmptyingAction(Component description, AEKey what, long maxAmount) {
}
