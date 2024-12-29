package appeng.util;

import appeng.api.networking.IGridNode;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.HolderLookup;

import java.io.IOException;

/**
 * Interface for objects that allow themselves to be exported to a debug export.
 */
public interface IDebugExportable {
    void debugExport(JsonWriter writer, HolderLookup.Provider registries, Reference2IntMap<Object> machineIds,
            Reference2IntMap<IGridNode> nodeIds)
            throws IOException;
}
