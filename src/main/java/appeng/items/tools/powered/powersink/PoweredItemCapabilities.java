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

package appeng.items.tools.powered.powersink;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import team.reborn.energy.api.EnergyStorage;

import appeng.api.config.Actionable;
import appeng.api.config.PowerUnit;
import appeng.api.implementations.items.IAEItemPowerStorage;

/**
 * The capability provider to expose chargable items to other mods.
 */
public class PoweredItemCapabilities implements EnergyStorage {

    private final ItemStack is;

    private final IAEItemPowerStorage item;

    public PoweredItemCapabilities(ItemStack is, IAEItemPowerStorage item) {
        this.is = is;
        this.item = item;
    }

    @Override
    public long insert(long maxReceive, TransactionContext transaction) {
        final double convertedOffer = PowerUnit.FE.convertTo(PowerUnit.AE, maxReceive);
        final double overflow = this.item.injectAEPower(this.is, convertedOffer, Actionable.MODULATE);
        return maxReceive - (int) PowerUnit.AE.convertTo(PowerUnit.FE, overflow);
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public long getAmount() {
        return (long) PowerUnit.AE.convertTo(PowerUnit.FE, this.item.getAECurrentPower(this.is));
    }

    @Override
    public long getCapacity() {
        return (long) PowerUnit.AE.convertTo(PowerUnit.FE, this.item.getAEMaxPower(this.is));
    }
}
