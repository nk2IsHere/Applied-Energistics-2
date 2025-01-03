/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2017, AlgorithmX2, All rights reserved.
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

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.config.PowerUnit;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;

public class FEP2PTunnelPart extends CapabilityP2PTunnelPart<FEP2PTunnelPart, EnergyStorage> {
    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_fe"));
    private static final EnergyStorage NULL_ENERGY_STORAGE = EnergyStorage.EMPTY;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public FEP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, EnergyStorage.SIDED);
        inputHandler = new InputEnergyStorage();
        outputHandler = new OutputEnergyStorage();
        emptyHandler = NULL_ENERGY_STORAGE;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    private class InputEnergyStorage implements EnergyStorage {

        @Override
        public long insert(long maxReceive, TransactionContext transactionContext) {
            var total = 0L;

            final var outputTunnels = FEP2PTunnelPart.this.getOutputs().size();

            if (outputTunnels == 0 | maxReceive == 0) {
                return 0;
            }

            final var amountPerOutput = maxReceive / outputTunnels;
            var overflow = amountPerOutput == 0 ? maxReceive : maxReceive % amountPerOutput;

            for (FEP2PTunnelPart target : FEP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final EnergyStorage output = capabilityGuard.get();
                    final var toSend = amountPerOutput + overflow;
                    final var received = output.insert(toSend, transactionContext);

                    overflow = toSend - received;
                    total += received;
                }
            }

            final var finalTotal = total;
            transactionContext.addCloseCallback((tx, result) -> {
                if (result.wasCommitted()) {
                    deductEnergyCost(finalTotal, PowerUnit.FE);
                }
            });

            return total;
        }

        @Override
        public long extract(long maxReceive, TransactionContext transactionContext) {
            return 0;
        }

        @Override
        public long getAmount() {
            var total = 0L;
            for (FEP2PTunnelPart t : FEP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = t.getAdjacentCapability()) {
                    total += capabilityGuard.get().getAmount();
                }
            }

            return total;
        }

        @Override
        public long getCapacity() {
            var total = 0L;
            for (FEP2PTunnelPart t : FEP2PTunnelPart.this.getOutputs()) {
                try (CapabilityGuard capabilityGuard = t.getAdjacentCapability()) {
                    total += capabilityGuard.get().getCapacity();
                }
            }

            return total;
        }

        @Override
        public boolean supportsExtraction() {
            return false;
        }
    }

    private class OutputEnergyStorage implements EnergyStorage {
        @Override
        public long insert(long maxExtract, TransactionContext transactionContext) {
            return 0;
        }

        @Override
        public long extract(long maxExtract, TransactionContext transactionContext) {
            try (CapabilityGuard input = getInputCapability()) {
                final var total = input.get().extract(maxExtract, transactionContext);
                transactionContext.addCloseCallback((tx, action) -> {
                    if (action.wasCommitted()) {
                        deductEnergyCost(total, PowerUnit.FE);
                    }
                });

                return total;
            }
        }

        @Override
        public long getAmount() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getAmount();
            }
        }

        @Override
        public long getCapacity() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getCapacity();
            }
        }

        @Override
        public boolean supportsInsertion() {
            return false;
        }
    }
}
