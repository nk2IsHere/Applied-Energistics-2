package appeng.hotkeys;

import appeng.api.features.HotkeyAction;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.ItemDefinition;
import appeng.items.tools.powered.AbstractPortableCell;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static appeng.api.features.HotkeyAction.*;

/**
 * Registry of {@link HotkeyAction}
 */
public class HotkeyActions {
    public static final Map<String, List<HotkeyAction>> REGISTRY = new HashMap<>();

    public static void init() {
        register(AEItems.WIRELESS_TERMINAL,
                (player, locator) -> AEItems.WIRELESS_TERMINAL.get().openFromInventory(player, locator),
                WIRELESS_TERMINAL);
        register(AEItems.WIRELESS_CRAFTING_TERMINAL,
                (player, locator) -> AEItems.WIRELESS_CRAFTING_TERMINAL.get().openFromInventory(player, locator),
                WIRELESS_TERMINAL);

        registerPortableCell(AEItems.PORTABLE_ITEM_CELL1K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL4K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL16K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL64K, PORTABLE_ITEM_CELL);
        registerPortableCell(AEItems.PORTABLE_ITEM_CELL256K, PORTABLE_ITEM_CELL);

        registerPortableCell(AEItems.PORTABLE_FLUID_CELL1K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL4K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL16K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL64K, PORTABLE_FLUID_CELL);
        registerPortableCell(AEItems.PORTABLE_FLUID_CELL256K, PORTABLE_FLUID_CELL);
    }

    /**
     * a convenience helper for registering hotkeys for portable cells
     */
    public static void registerPortableCell(ItemDefinition<? extends AbstractPortableCell> cell, String id) {
        register(cell, (player, locator) -> cell.get().openFromInventory(player, locator), id);
    }

    /**
     * a convenience Helper for registering Hotkeys for both the Inventory and Curios (if applicable)
     */
    public static void register(ItemLike item, InventoryHotkeyAction.Opener opener, String id) {
        register(new InventoryHotkeyAction(item, opener), id);
    }

    /**
     * see {@link HotkeyAction#register(HotkeyAction, String)}
     */
    public static synchronized void register(HotkeyAction hotkeyAction, String id) {
        if (REGISTRY.containsKey(id)) {
            REGISTRY.get(id).addFirst(hotkeyAction);
        } else {
            REGISTRY.put(id, new ArrayList<>(List.of(hotkeyAction)));
            AppEng.instance().registerHotkey(id);
        }
    }
}
