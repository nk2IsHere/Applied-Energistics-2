package appeng.api;

import appeng.api.implementations.blockentities.ICrankable;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import appeng.core.AppEng;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.core.Direction;

public final class AECapabilities {

    public static final BlockApiLookup<IInWorldGridNodeHost, Void> IN_WORLD_GRID_NODE_HOST = BlockApiLookup.get(
        AppEng.makeId("iinworldgridnodehost"),
        IInWorldGridNodeHost.class, Void.class);

    public static final BlockApiLookup<ICrankable, Direction> CRANKABLE = BlockApiLookup.get(AppEng.makeId("crankable"),
        ICrankable.class, Direction.class);

    public static final BlockApiLookup<MEStorage, Direction> ME_STORAGE = BlockApiLookup.get(AppEng.makeId("me_storage"),
        MEStorage.class, Direction.class);
}
