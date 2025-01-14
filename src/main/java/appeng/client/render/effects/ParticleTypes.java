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

package appeng.client.render.effects;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class ParticleTypes {

    private ParticleTypes() {
    }

    public static final SimpleParticleType CRAFTING = FabricParticleTypes.simple();
    public static final ParticleType<EnergyParticleData> ENERGY = new ParticleType<>(false) {
        @Override
        public MapCodec<EnergyParticleData> codec() {
            return EnergyParticleData.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, EnergyParticleData> streamCodec() {
            return EnergyParticleData.STREAM_CODEC;
        }
    };
    public static final ParticleType<LightningArcParticleData> LIGHTNING_ARC = new ParticleType<>(false) {
        @Override
        public MapCodec<LightningArcParticleData> codec() {
            return LightningArcParticleData.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, LightningArcParticleData> streamCodec() {
            return LightningArcParticleData.STREAM_CODEC;
        }
    };
    public static final SimpleParticleType LIGHTNING = FabricParticleTypes.simple();
    public static final SimpleParticleType MATTER_CANNON = FabricParticleTypes.simple();
    public static final SimpleParticleType VIBRANT = FabricParticleTypes.simple();

}
