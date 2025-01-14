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

package appeng.core.definitions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.EntityType.EntityFactory;
import net.minecraft.world.entity.MobCategory;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;

import appeng.core.AppEng;
import appeng.entity.TinyTNTPrimedEntity;

public final class AEEntities {

    public static final DeferredRegister<EntityType<?>> DR = DeferredRegister.create(AppEng.MOD_ID,
            Registries.ENTITY_TYPE);

    public static final Map<String, String> ENTITY_ENGLISH_NAMES = new HashMap<>();

    public static final RegistrySupplier<EntityType<TinyTNTPrimedEntity>> TINY_TNT_PRIMED = create(
            "tiny_tnt_primed",
            "Tiny TNT Primed",
            TinyTNTPrimedEntity::new,
            MobCategory.MISC,
            builder -> builder.clientTrackingRange(16).updateInterval(4).alwaysUpdateVelocity(true));

    private static <T extends Entity> RegistrySupplier<EntityType<T>> create(String id,
            String englishName,
            EntityFactory<T> entityFactory,
            MobCategory classification,
            Consumer<Builder<T>> customizer) {
        ENTITY_ENGLISH_NAMES.put(id, englishName);
        return DR.register(id, () -> {
            Builder<T> builder = Builder.of(entityFactory, classification);
            customizer.accept(builder);
            // Temporarily disable the data fixer check to avoid the annoying "no data fixer registered for ae2:xxx".
            boolean prev = SharedConstants.CHECK_DATA_FIXER_SCHEMA;
            SharedConstants.CHECK_DATA_FIXER_SCHEMA = false;
            EntityType<T> result = builder.build(id);
            SharedConstants.CHECK_DATA_FIXER_SCHEMA = prev;
            return result;
        });
    }

}
