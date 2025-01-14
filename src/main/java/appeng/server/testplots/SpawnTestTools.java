package appeng.server.testplots;

import java.util.List;

import net.minecraft.core.GlobalPos;

import appeng.api.config.Actionable;
import appeng.api.features.GridLinkables;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.core.definitions.AEItems;

public final class SpawnTestTools {

    public static void init() {
        SpawnExtraGridTestTools.EVENT.register(SpawnTestTools::spawnWirelessTerminals);
    }

    public static void spawnWirelessTerminals(SpawnExtraGridTestTools e) {
        // Find a suitable WAP to link to
        var waps = e.getGrid().getMachines(WirelessAccessPointBlockEntity.class);
        if (waps.isEmpty()) {
            return;
        }

        var wap = waps.iterator().next();
        var inventory = e.getInventory();

        for (var item : List.of(AEItems.WIRELESS_CRAFTING_TERMINAL, AEItems.WIRELESS_TERMINAL)) {
            var terminal = item.stack();
            // Fully charge it
            item.get().injectAEPower(terminal, Double.MAX_VALUE, Actionable.MODULATE);
            // Link it to the WAP we just placed
            GridLinkables.get(item).link(terminal, GlobalPos.of(wap.getLevel().dimension(), wap.getBlockPos()));
            inventory.addItems(terminal);
        }
    }

}
