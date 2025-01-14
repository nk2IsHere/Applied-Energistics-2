/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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

package appeng.parts.p2p;

import java.util.Iterator;
import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;

public class ItemP2PTunnelPart extends CapabilityP2PTunnelPart<ItemP2PTunnelPart, Storage<ItemVariant>> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_items"));
    private static final Storage<ItemVariant> NULL_ITEM_HANDLER = Storage.empty();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public ItemP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ItemStorage.SIDED);
        inputHandler = new InputItemHandler();
        outputHandler = new OutputItemHandler();
        emptyHandler = NULL_ITEM_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputItemHandler implements InsertionOnlyStorage<ItemVariant> {

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            var remainder = maxAmount;

            final var outputTunnels = ItemP2PTunnelPart.this.getOutputs().size();
            if (outputTunnels == 0 || maxAmount == 0) {
                return 0;
            }

            final var amountPerOutput = maxAmount / outputTunnels;
            var overflow = amountPerOutput == 0 ? maxAmount : maxAmount % amountPerOutput;

            for (ItemP2PTunnelPart target : ItemP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final var output = capabilityGuard.get();
                    final var toSend = amountPerOutput + overflow;

                    if (toSend <= 0) {
                        // Both overflow and amountPerOutput are 0, so they will be for further outputs as well.
                        break;
                    }

                    final var received = output.insert(resource, toSend, transaction);
                    final var sent = toSend - received;

                    overflow = received;
                    remainder -= sent;
                }
            }

            final var finalRemainder = remainder;
            transaction.addCloseCallback((tx, result) -> {
                if (result.wasCommitted()) {
                    deductTransportCost(maxAmount - finalRemainder, AEKeyType.items());
                }
            });

            return remainder;
        }
    }

    private class OutputItemHandler implements ExtractionOnlyStorage<ItemVariant> {

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            try (CapabilityGuard input = getInputCapability()) {
                final var result = input.get().extract(resource, maxAmount, transaction);
                transaction.addCloseCallback((tx, action) -> {
                    if (action.wasCommitted()) {
                        deductTransportCost(result, AEKeyType.items());
                    }
                });

                return result;
            }
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().iterator();
            }
        }
    }
}
