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

package appeng.client.render.cablebus;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.util.AEColor;
import appeng.integration.fabric.IFabricBakedModel;
import appeng.util.Platform;

public class P2PTunnelFrequencyBakedModel implements IFabricBakedModel {

    private final TextureAtlasSprite texture;

    private final static Cache<Long, Mesh> modelCache = CacheBuilder.newBuilder().maximumSize(100).build();

    private static final int[][] QUAD_OFFSETS = new int[][] { { 3, 11, 2 }, { 11, 11, 2 }, { 3, 3, 2 }, { 11, 3, 2 } };

    public P2PTunnelFrequencyBakedModel(TextureAtlasSprite texture) {
        this.texture = texture;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
            Supplier<RandomSource> randomSupplier, RenderContext context) {
        var modelData = blockView.getBlockEntityRenderData(pos);
        if (!(modelData instanceof Long frequency)) {
            return;
        }

        var frequencyMesh = getFrequencyModel(frequency);
        if (frequencyMesh != null) {
            frequencyMesh.outputTo(context.getEmitter());
        }
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    private Mesh createFrequencyMesh(short frequency, boolean active) {

        MeshBuilder meshBuilder = renderer.meshBuilder();

        final AEColor[] colors = Platform.p2p().toColors(frequency);
        final CubeBuilder cb = new CubeBuilder(meshBuilder.getEmitter());

        cb.setTexture(this.texture);
        cb.setEmissiveMaterial(active);

        for (int i = 0; i < 4; ++i) {
            final int[] offs = QUAD_OFFSETS[i];
            for (int j = 0; j < 4; ++j) {
                final AEColor col = colors[j];

                if (active) {
                    cb.setColorRGB(col.mediumVariant);
                } else {
                    final float scale = 0.3f / 255.0f;
                    cb.setColorRGB((col.blackVariant >> 16 & 0xff) * scale,
                            (col.blackVariant >> 8 & 0xff) * scale, (col.blackVariant & 0xff) * scale);
                }

                final int startx = j % 2;
                final int starty = 1 - j / 2;

                cb.addCube(offs[0] + startx, offs[1] + starty, offs[2], offs[0] + startx + 1, offs[1] + starty + 1,
                        offs[2] + 1);
            }

        }

        // Reset back to default
        cb.setEmissiveMaterial(false);

        return meshBuilder.build();
    }

    private Mesh getFrequencyModel(long partFlags) {
        try {
            return modelCache.get(partFlags, () -> {
                short frequency = (short) (partFlags & 0xffffL);
                boolean active = (partFlags & 0x10000L) != 0;
                return this.createFrequencyMesh(frequency, active);
            });
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;// TODO
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.texture;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
