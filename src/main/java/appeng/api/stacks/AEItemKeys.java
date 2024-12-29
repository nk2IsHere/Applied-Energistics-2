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

package appeng.api.stacks;

import appeng.core.AppEng;
import appeng.core.localization.GuiText;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Objects;
import java.util.stream.Stream;

final class AEItemKeys extends AEKeyType {
    private static final ResourceLocation ID = AppEng.makeId("i");

    static final AEItemKeys INSTANCE = new AEItemKeys();

    private AEItemKeys() {
        super(ID, AEItemKey.class, GuiText.Items.text());
    }

    @Override
    public MapCodec<? extends AEKey> codec() {
        return AEItemKey.MAP_CODEC;
    }

    @Override
    public AEItemKey readFromPacket(RegistryFriendlyByteBuf input) {
        Objects.requireNonNull(input);

        return AEItemKey.fromPacket(input);
    }

    @Override
    public AEItemKey loadKeyFromTag(HolderLookup.Provider registries, CompoundTag tag) {
        return AEItemKey.fromTag(registries, tag);
    }

    @Override
    public boolean supportsFuzzyRangeSearch() {
        return true;
    }

    @Override
    public Stream<TagKey<?>> getTagNames() {
        return BuiltInRegistries.ITEM.getTagNames().map(t -> t);
    }
}
