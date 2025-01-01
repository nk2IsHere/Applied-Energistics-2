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

package appeng.client.render;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A quaint model provider that provides a single model with a single given resource identifier.
 */
public class SimpleModelLoader<T extends UnbakedModel> implements ModelResolver {

    private final ResourceLocation identifier;

    private final Supplier<T> factory;

    public SimpleModelLoader(ResourceLocation identifier, Supplier<T> factory) {
        this.factory = factory;
        this.identifier = identifier;
    }

    @Override
    public @Nullable UnbakedModel resolveModel(Context context) {
        if (context.id().equals(this.identifier)) {
            return factory.get();
        } else {
            return null;
        }
    }
}
