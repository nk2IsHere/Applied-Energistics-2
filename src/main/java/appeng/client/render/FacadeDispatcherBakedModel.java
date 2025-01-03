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

package appeng.client.render;

import java.util.Map;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.ItemStackMap;

import appeng.client.render.cablebus.FacadeBuilder;
import appeng.items.parts.FacadeItem;

/**
 * This baked model class is used as a dispatcher to redirect the renderer to the *real* model that should be used based
 * on the item stack. A custom Item Override List is used to accomplish this.
 */
// TODO: Figure out how to implement forge cache
public class FacadeDispatcherBakedModel extends DelegateBakedModel {
    private final FacadeBuilder facadeBuilder;
    private final Map<ItemStack, FacadeBakedItemModel> cache = ItemStackMap.createTypeAndTagMap();

    public FacadeDispatcherBakedModel(BakedModel baseModel, FacadeBuilder facadeBuilder) {
        super(baseModel);
        this.facadeBuilder = facadeBuilder;
    }

    @Override
    public void emitBlockQuads(
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            RenderContext context) {
        var model = cache.get(ItemStack.EMPTY);
        if (model == null) {
            model = new FacadeBakedItemModel(getBaseModel(), ItemStack.EMPTY, facadeBuilder);
            cache.put(ItemStack.EMPTY, model);
        }

        model.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(
            ItemStack stack,
            Supplier<RandomSource> randomSupplier,
            RenderContext context) {
        if (!(stack.getItem() instanceof FacadeItem itemFacade)) {
            return;
        }

        var textureItem = itemFacade.getTextureItem(stack);
        var model = cache.get(textureItem);
        if (model == null) {
            model = new FacadeBakedItemModel(getBaseModel(), textureItem, facadeBuilder);
            cache.put(textureItem, model);
        }

        model.emitItemQuads(stack, randomSupplier, context);
    }
}
