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

package appeng.blockentity.storage;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.BlankVariantView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.*;
import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.implementations.blockentities.IMEChest;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.events.GridPowerStorageStateChanged;
import appeng.api.networking.events.GridPowerStorageStateChanged.PowerEventType;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.*;
import appeng.api.storage.cells.CellState;
import appeng.api.storage.cells.StorageCell;
import appeng.api.util.AEColor;
import appeng.api.util.IConfigManager;
import appeng.api.util.KeyTypeSelection;
import appeng.api.util.KeyTypeSelectionHost;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkedPoweredBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.IPriorityHost;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.DelegatingMEInventory;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.MEChestMenu;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.items.BasicCellChestMenu;
import appeng.util.Platform;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;

public class MEChestBlockEntity extends AENetworkedPoweredBlockEntity
        implements IMEChest, ITerminalHost, IPriorityHost, IColorableBlockEntity,
        ServerTickingBlockEntity, IStorageProvider, KeyTypeSelectionHost {

    private static final Logger LOG = LoggerFactory.getLogger(MEChestBlockEntity.class);

    private final AppEngInternalInventory inputInventory = new AppEngInternalInventory(this, 1);
    private final AppEngInternalInventory cellInventory = new AppEngInternalInventory(this, 1);
    private final InternalInventory internalInventory = new CombinedInternalInventory(this.inputInventory,
            this.cellInventory);

    private final IActionSource mySrc = new MachineSource(this);
    private final IConfigManager config = IConfigManager.builder(this::saveChanges)
            .registerSetting(Settings.SORT_BY, SortOrder.NAME)
            .registerSetting(Settings.VIEW_MODE, ViewItems.ALL)
            .registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING)
            .build();
    private final KeyTypeSelection keyTypeSelection = new KeyTypeSelection(this::saveChanges, keyType -> true);
    private int priority = 0;
    // Client-side cell state or last cell-state sent to client (for update-checking)
    private CellState clientCellState = CellState.ABSENT;
    // Client-side cached powered state or last powered state sent to client
    private boolean clientPowered;
    // This is only used on the client to display the right cell model without
    // synchronizing the entire cell's inventory when a chest comes into view.
    private Item cellItem = Items.AIR;
    private boolean wasOnline = false;
    private AEColor paintedColor = AEColor.TRANSPARENT;
    private boolean isCached = false;
    private ChestMonitorHandler cellHandler;
    private Storage<FluidVariant> fluidHandler;
    private SupplierStorage supplierStorage;
    private double idlePowerUsage;

    public MEChestBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
        this.setInternalMaxPower(PowerMultiplier.CONFIG.multiply(500));
        this.getMainNode()
                .addService(IStorageProvider.class, this)
                .setFlags(GridFlags.REQUIRE_CHANNEL);

        this.setInternalPublicPowerStorage(true);
        this.setInternalPowerFlow(AccessRestriction.WRITE);

        this.inputInventory.setFilter(new InputInventoryFilter());
        this.cellInventory.setFilter(new CellInventoryFilter());
    }

    public ItemStack getCell() {
        return this.cellInventory.getStackInSlot(0);
    }

    public void setCell(ItemStack stack) {
        this.cellInventory.setItemDirect(0, Objects.requireNonNull(stack));
    }

    @Override
    protected void emitPowerStateEvent(PowerEventType x) {
        if (x == PowerEventType.RECEIVE_POWER) {
            this.getMainNode().ifPresent(
                    grid -> grid.postEvent(new GridPowerStorageStateChanged(this, PowerEventType.RECEIVE_POWER)));
        } else {
            this.recalculateDisplay();
        }
    }

    private void recalculateDisplay() {
        boolean changed = false;

        var cellState = this.getCellStatus(0);
        if (clientCellState != cellState) {
            clientCellState = cellState;
            changed = true;
        }

        var powered = isPowered();
        if (clientPowered != powered) {
            clientPowered = powered;
            changed = true;
        }

        if (changed) {
            this.markForUpdate();
        }
    }

    @Override
    public int getCellCount() {
        return 1;
    }

    private void updateHandler() {
        if (!this.isCached) {
            this.cellHandler = null;
            this.fluidHandler = null;
            this.supplierStorage = null;

            var is = this.getCell();
            if (!is.isEmpty()) {
                this.isCached = true;
                var newCell = StorageCells.getCellInventory(is, this::onCellContentChanged);
                if (newCell != null) {
                    idlePowerUsage = 1.0 + newCell.getIdleDrain();
                    this.cellHandler = this.wrap(newCell);

                    this.getMainNode().setIdlePowerUsage(idlePowerUsage);

                    if (this.cellHandler != null) {
                        this.fluidHandler = new FluidHandler();
                        // Return a supplier here so that if the cell is removed *between menu ticks*
                        // items can no longer be inserted or removed. On the next normal tick
                        // the menu will then close.
                        this.supplierStorage = new SupplierStorage(() -> this.cellHandler);
                    }
                }
            }
        }
    }

    private ChestMonitorHandler wrap(StorageCell cellInventory) {
        if (cellInventory == null) {
            return null;
        }

        return new ChestMonitorHandler(cellInventory);
    }

    @Override
    public ILinkStatus getLinkStatus() {
        updateHandler();
        if (cellHandler == null) {
            return ILinkStatus.ofDisconnected(PlayerMessages.ChestCannotReadStorageCell.text());
        }
        // The Chest GUI works independently of the grid the chest may be connected to
        if (!isPowered()) {
            return ILinkStatus.ofDisconnected(GuiText.OutOfPower.text());
        }
        return ILinkStatus.ofConnected();
    }

    @Override
    public CellState getCellStatus(int slot) {
        if (isClientSide()) {
            return clientCellState;
        }

        this.updateHandler();

        var cell = this.getCell();
        var ch = StorageCells.getHandler(cell);

        if (this.cellHandler != null && ch != null) {
            return this.cellHandler.cellInventory.getStatus();
        }

        return CellState.ABSENT;
    }

    @Nullable
    @Override
    public Item getCellItem(int slot) {
        if (slot != 0) {
            return null;
        }
        // Client-side we'll need to actually use the synced state
        if (level == null || level.isClientSide) {
            return cellItem;
        }
        ItemStack cell = getCell();
        return cell.isEmpty() ? null : cell.getItem();
    }

    @Override
    public @Nullable MEStorage getCellInventory(int slot) {
        if (slot == 0 && this.cellHandler != null) {
            return this.cellHandler;
        }
        return null;
    }

    @Nullable
    @Override
    public StorageCell getOriginalCellInventory(int slot) {
        if (slot == 0 && this.cellHandler != null) {
            return this.cellHandler.cellInventory;
        }
        return null;
    }

    @Override
    public boolean isPowered() {
        if (isClientSide()) {
            return clientPowered;
        }

        if (getMainNode().isPowered()) {
            return true;
        }

        return getAECurrentPower() > 1;
    }

    @Override
    public boolean isCellBlinking(int slot) {
        return false;
    }

    @Override
    protected double extractAEPower(double amt, Actionable mode) {
        double stash = 0.0;

        var grid = getMainNode().getGrid();
        if (grid != null) {
            var eg = grid.getEnergyService();
            stash = eg.extractAEPower(amt, mode, PowerMultiplier.ONE);
            if (stash >= amt) {
                return stash;
            }
        }

        // local battery!
        return super.extractAEPower(amt - stash, mode) + stash;
    }

    @Override
    public void serverTick() {
        var grid = getMainNode().getGrid();

        // Handle energy-use when not grid-powered
        if (grid == null || !grid.getEnergyService().isNetworkPowered()) {
            this.extractAEPower(idlePowerUsage, Actionable.MODULATE, PowerMultiplier.CONFIG);
            this.recalculateDisplay();
        }

        if (!this.inputInventory.isEmpty()) {
            this.tryToStoreContents();
        }
    }

    @Override
    protected void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);

        data.writeEnum(clientCellState = getCellStatus(0));
        data.writeBoolean(clientPowered = isPowered());
        data.writeByte(paintedColor.ordinal());

        // Note that we trust that the change detection in recalculateDisplay will trip
        // when it changes from
        // empty->non-empty, so when the cell is changed, it should re-send the state
        // because of that
        data.writeVarInt(Item.getId(getCell().getItem()));
    }

    @Override
    protected boolean readFromStream(RegistryFriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);

        var oldCellState = clientCellState;
        var oldPowered = clientPowered;
        var oldColor = paintedColor;
        var oldCellItem = cellItem;

        clientCellState = data.readEnum(CellState.class);
        clientPowered = data.readBoolean();
        paintedColor = data.readEnum(AEColor.class);
        cellItem = Item.byId(data.readVarInt());

        return c
                || oldCellState != clientCellState
                || oldPowered != clientPowered
                || oldColor != paintedColor
                || oldCellItem != cellItem;
    }

    @Override
    protected void saveVisualState(CompoundTag data) {
        super.saveVisualState(data);

        data.putBoolean("powered", isPowered());
        data.putString("cellStatus", getCellStatus(0).name());
        var itemId = BuiltInRegistries.ITEM.getKey(getCell().getItem());
        data.putString("cellId", itemId.toString());
        data.putString("color", paintedColor.name());
    }

    @Override
    protected void loadVisualState(CompoundTag data) {
        super.loadVisualState(data);

        this.clientPowered = data.getBoolean("powered");

        try {
            this.clientCellState = CellState.valueOf(data.getString("cellStatus"));
        } catch (Exception e) {
            this.clientCellState = CellState.ABSENT;
            LOG.warn("Couldn't read cell status for {} from {}", this, data);
        }

        try {
            this.cellItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.getString("cellId")));
        } catch (Exception e) {
            LOG.warn("Couldn't read cell item for {} from {}", this, data);
            this.cellItem = Items.AIR;
        }

        try {
            this.paintedColor = AEColor.valueOf(data.getString("color"));
        } catch (IllegalArgumentException ignore) {
            LOG.warn("Invalid painted color in visual data for {}: {}", this, data);
            this.paintedColor = AEColor.TRANSPARENT;
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        this.config.readFromNBT(data, registries);
        this.keyTypeSelection.readFromNBT(data, registries);
        this.priority = data.getInt("priority");
        if (data.contains("paintedColor")) {
            this.paintedColor = AEColor.values()[data.getByte("paintedColor")];
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        this.config.writeToNBT(data, registries);
        this.keyTypeSelection.writeToNBT(data);
        data.putInt("priority", this.priority);
        data.putByte("paintedColor", (byte) this.paintedColor.ordinal());
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        var currentOnline = this.getMainNode().isOnline();
        if (this.wasOnline != currentOnline) {
            this.wasOnline = currentOnline;
            IStorageProvider.requestUpdate(getMainNode());
            recalculateDisplay();
        }
    }

    @Override
    public MEStorage getInventory() {
        return new SupplierStorage(() -> {
            updateHandler();
            return this.cellHandler;
        });
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.internalInventory;
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (inv == this.cellInventory) {
            this.cellHandler = null;
            this.isCached = false; // recalculate the storage cell.

            IStorageProvider.requestUpdate(getMainNode());

            // update the neighbors
            if (this.level != null) {
                Platform.notifyBlocksOfNeighbors(this.level, this.worldPosition);
                this.markForUpdate();
            }
        }
        if (inv == this.inputInventory && !inv.getStackInSlot(slot).isEmpty()) {
            this.tryToStoreContents();
        }
    }

    @Override
    public InternalInventory getExposedInventoryForSide(Direction side) {
        if (side == this.getFront()) {
            return this.cellInventory;
        } else {
            return this.inputInventory;
        }
    }

    private void tryToStoreContents() {

        if (!this.inputInventory.isEmpty()) {
            this.updateHandler();

            if (this.cellHandler != null) {
                var stack = this.inputInventory.getStackInSlot(0);
                if (stack.isEmpty()) {
                    return;
                }

                var inserted = StorageHelper.poweredInsert(this, this.cellHandler,
                        AEItemKey.of(stack), stack.getCount(), this.mySrc);

                if (inserted >= stack.getCount()) {
                    this.inputInventory.setItemDirect(0, ItemStack.EMPTY);
                } else {
                    stack.shrink((int) inserted);
                    this.inputInventory.setItemDirect(0, stack);
                }
            }
        }
    }

    @Override
    public void mountInventories(IStorageMounts storageMounts) {
        if (this.getMainNode().isOnline()) {
            this.updateHandler();

            if (this.cellHandler != null) {
                storageMounts.mount(this.cellHandler, priority);
            }
        }
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int newValue) {
        this.priority = newValue;
        this.cellHandler = null;
        this.isCached = false; // recalculate the storage cell.

        IStorageProvider.requestUpdate(getMainNode());
    }

    private void blinkCell(int slot) {
        this.recalculateDisplay();
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.config;
    }

    @Override
    public KeyTypeSelection getKeyTypeSelection() {
        return this.keyTypeSelection;
    }

    public boolean openGui(Player p) {
        this.updateHandler();
        if (this.cellHandler != null) {
            var ch = StorageCells.getHandler(this.getCell());

            if (ch != null) {
                MenuOpener.open(BasicCellChestMenu.TYPE, p, MenuLocators.forBlockEntity(this));
                return true;
            }
        }

        return false;
    }

    @Override
    public AEColor getColor() {
        return this.paintedColor;
    }

    @Override
    public boolean recolourBlock(Direction side, AEColor newPaintedColor, Player who) {
        if (this.paintedColor == newPaintedColor) {
            return false;
        }

        this.paintedColor = newPaintedColor;
        this.saveChanges();
        this.markForUpdate();
        return true;
    }

    private void onCellContentChanged() {
        if (cellHandler != null) {
            cellHandler.cellInventory.persist();
        }
        this.level.blockEntityChanged(this.worldPosition);
    }

    public void openCellInventoryMenu(Player player) {
        MenuOpener.open(MEChestMenu.TYPE, player, MenuLocators.forBlockEntity(this));
    }

    private class ChestMonitorHandler extends DelegatingMEInventory {
        private final StorageCell cellInventory;

        public ChestMonitorHandler(StorageCell cellInventory) {
            super(cellInventory);
            this.cellInventory = cellInventory;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            var inserted = super.insert(what, amount, mode, source);
            if (inserted > 0 && mode == Actionable.MODULATE) {
                blinkCell(0);
            }
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            var extracted = super.extract(what, amount, mode, source);
            if (extracted > 0 && mode == Actionable.MODULATE) {
                blinkCell(0);
            }
            return extracted;
        }
    }

    @Nullable
    public Storage<FluidVariant> getFluidHandler(Direction side) {
        if (side != getFront()) {
            return fluidHandler;
        } else {
            return null;
        }
    }

    @Nullable
    public MEStorage getMEStorage(Direction side) {
        if (side != getFront()) {
            return getInventory();
        } else {
            return null;
        }
    }

    private class FluidHandler extends SnapshotParticipant<Boolean>
            implements InsertionOnlyStorage<FluidVariant> {
        private GenericStack queuedInsert;

        /**
         * If we accept fluids, simulate that we have an empty tank with 1 bucket capacity at all times.
         */
        private final List<StorageView<FluidVariant>> fakeInputTanks = Collections.singletonList(
                new BlankVariantView<>(FluidVariant.blank(), AEFluidKey.AMOUNT_BUCKET));

        private boolean canAcceptLiquids() {
            return MEChestBlockEntity.this.cellHandler != null;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);

            if (queuedInsert != null) {
                return 0; // Can only insert once per action
            }

            MEChestBlockEntity.this.updateHandler();
            if (canAcceptLiquids()) {
                var what = AEFluidKey.of(resource.getFluid());
                var inserted = pushToNetwork(what, maxAmount, Actionable.SIMULATE);
                if (inserted > 0) {
                    updateSnapshots(transaction);
                    queuedInsert = new GenericStack(what, inserted);
                }
                return inserted;
            }
            return 0;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            if (canAcceptLiquids()) {
                return fakeInputTanks.iterator();
            } else {
                return Collections.emptyIterator();
            }
        }

        @Override
        protected final Boolean createSnapshot() {
            // Null snapshots are not allowed even though this is what we really want, so we just use Boolean instead.
            return Boolean.TRUE;
        }

        @Override
        protected final void readSnapshot(Boolean snapshot) {
            queuedInsert = null;
        }

        @Override
        protected final void onFinalCommit() {
            pushToNetwork(queuedInsert.what(), queuedInsert.amount(), Actionable.MODULATE);
            queuedInsert = null;
        }

        private long pushToNetwork(AEKey what, long amount, Actionable mode) {
            MEChestBlockEntity.this.updateHandler();
            if (canAcceptLiquids()) {
                return StorageHelper.poweredInsert(
                        MEChestBlockEntity.this,
                        MEChestBlockEntity.this.cellHandler,
                        what,
                        amount,
                        MEChestBlockEntity.this.mySrc,
                        mode);
            }
            return 0;
        }
    }

    private class InputInventoryFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return false;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (isPowered()) {
                updateHandler();
                if (cellHandler == null) {
                    return false;
                }

                var what = AEItemKey.of(stack);
                if (what == null) {
                    return false;
                }

                return cellHandler.insert(what, stack.getCount(), Actionable.SIMULATE, mySrc) > 0;
            }
            return false;
        }
    }

    private static class CellInventoryFilter implements IAEItemFilter {

        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return StorageCells.getHandler(stack) != null;
        }

    }

    @Override
    public ItemStack getMainMenuIcon() {
        return AEBlocks.ME_CHEST.stack();
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(MEChestMenu.TYPE, player, MenuLocators.forBlockEntity(this));
    }
}
