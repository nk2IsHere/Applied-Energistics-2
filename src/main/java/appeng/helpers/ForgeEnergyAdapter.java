/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
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

package appeng.helpers;

import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.blockentity.powersink.IExternalPowerSink;
import appeng.me.energy.StoredEnergyAmount;

/**
 * Adapts an {@link IExternalPowerSink} to TR Energy's {@link EnergyStorage} by buffering energy packets. Not ideal, but
 * easier than rewriting all of the power system to support transactions.
 */
public class ForgeEnergyAdapter extends SnapshotParticipant<Double> implements EnergyStorage {

    private final IExternalPowerSink sink;
    private double buffer = 0;

    public ForgeEnergyAdapter(IExternalPowerSink sink) {
        this.sink = sink;
    }

    @Override
    protected Double createSnapshot() {
        return buffer;
    }

    @Override
    protected void readSnapshot(Double snapshot) {
        buffer = snapshot;
    }

    @Override
    protected void onFinalCommit() {
        buffer = sink.injectExternalPower(PowerUnit.FE, buffer, Actionable.MODULATE);
        if (buffer < StoredEnergyAmount.MIN_AMOUNT) {
            // Prevent a small leftover amount from blocking further energy insertions.
            buffer = 0;
        }
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notNegative(maxAmount);
        // Always schedule a push into the network at outer commit.
        updateSnapshots(transaction);

        if (buffer == 0) {
            // Cap at the remaining capacity...
            maxAmount = (long) Math
                    .floor(Math.min(maxAmount, this.sink.getExternalPowerDemand(PowerUnit.FE, maxAmount)));
            buffer = maxAmount;
            return maxAmount;
        }

        return 0;
    }

    @Override
    public final long getAmount() {
        return (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, this.sink.getAECurrentPower()));
    }

    @Override
    public final long getCapacity() {
        return (long) Math.floor(PowerUnit.AE.convertTo(PowerUnit.FE, this.sink.getAEMaxPower()));
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public boolean supportsExtraction() {
        return false;
    }

}
