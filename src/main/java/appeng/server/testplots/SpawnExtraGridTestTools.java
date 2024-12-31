package appeng.server.testplots;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Triggered to spawn additional testing tools into a container placed next to a spawned AE2 grid.
 */
@TestPlotClass
public class SpawnExtraGridTestTools {

    public static final Event<Consumer<SpawnExtraGridTestTools>> EVENT = EventFactory.createArrayBacked(
        Consumer.class,
        (events) -> (data) -> {
            for (var event : events) {
                event.accept(data);
            }
        });

    private final ResourceLocation plotId;
    private final InternalInventory inventory;
    private final IGrid grid;

    public SpawnExtraGridTestTools(ResourceLocation plotId, InternalInventory inventory, IGrid grid) {
        this.plotId = plotId;
        this.inventory = inventory;
        this.grid = grid;
    }

    public ResourceLocation getPlotId() {
        return plotId;
    }

    public InternalInventory getInventory() {
        return inventory;
    }

    public IGrid getGrid() {
        return grid;
    }
}
