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

package appeng.parts.p2p;

import java.util.List;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;

public class FluidP2PTunnelPart extends CapabilityP2PTunnelPart<FluidP2PTunnelPart, Storage<FluidVariant>> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fluids"));
    private static final Storage<FluidVariant> NULL_FLUID_HANDLER = Storage.empty();

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public FluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, FluidStorage.SIDED);
        inputHandler = new InputFluidHandler();
        outputHandler = new OutputFluidHandler();
        emptyHandler = NULL_FLUID_HANDLER;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputFluidHandler implements SingleSlotStorage<FluidVariant> {

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            var total = 0L;
            final int outputTunnels = FluidP2PTunnelPart.this.getOutputs().size();
            if (outputTunnels == 0 || maxAmount == 0) {
                return 0L;
            }

            final var amountPerOutput = maxAmount / outputTunnels;
            var overflow = amountPerOutput == 0 ? maxAmount : maxAmount % amountPerOutput;

            for (FluidP2PTunnelPart target : FluidP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final var output = capabilityGuard.get();
                    final var toSend = amountPerOutput + overflow;
                    final var received = output.extract(resource, toSend, transaction);

                    overflow = toSend - received;
                    total += received;
                }
            }

            final var finalTotal = total;
            transaction.addCloseCallback((tx, result) -> {
                if (result.wasCommitted()) {
                    deductTransportCost(finalTotal, AEKeyType.fluids());
                }
            });

            return total;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public boolean isResourceBlank() {
            return true;
        }

        @Override
        public FluidVariant getResource() {
            return FluidVariant.blank();
        }

        @Override
        public long getAmount() {
            return 0;
        }

        @Override
        public long getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }
    }

    private class OutputFluidHandler implements SingleSlotStorage<FluidVariant> {

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            try (CapabilityGuard input = getInputCapability()) {
                final var result = input.get().extract(resource, maxAmount, transaction);
                transaction.addCloseCallback((tx, action) -> {
                    if (action.wasCommitted()) {
                        deductTransportCost(result, AEKeyType.fluids());
                    }
                });

                return result;
            }
        }

        @Override
        public boolean isResourceBlank() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().iterator().next().isResourceBlank();
            }
        }

        @Override
        public FluidVariant getResource() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().iterator().next().getResource();
            }
        }

        @Override
        public long getAmount() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().iterator().next().getAmount();
            }
        }

        @Override
        public long getCapacity() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().iterator().next().getCapacity();
            }
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }
    }
}
