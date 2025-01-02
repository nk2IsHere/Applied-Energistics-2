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

package appeng.client.render.model;

import appeng.hooks.CompassManager;
import appeng.integration.abstraction.IFabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

/**
 * This baked model combines the quads of a compass base and the quads of a compass pointer, which will be rotated
 * around the Y-axis to get the compass to point in the right direction.
 */
public class MeteoriteCompassBakedModel implements IFabricBakedModel {
    // Rotation is expressed as radians

    private final BakedModel base;

    private final BakedModel pointer;

    private float fallbackRotation = 0;

    public MeteoriteCompassBakedModel(BakedModel base, BakedModel pointer) {
        this.base = base;
        this.pointer = pointer;
    }

    public BakedModel getPointer() {
        return pointer;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos,
            Supplier<RandomSource> randomSupplier, RenderContext context) {
        // Pre-compute the quad count to avoid list resizes
        this.base.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        this.base.emitItemQuads(stack, randomSupplier, context);

        // This is used to render a compass pointing in a specific direction when being
        // held in hand
        // Set up the rotation around the Y-axis for the pointer
        context.pushTransform(quad -> {
            Quaternionf quaternion = new Quaternionf().rotationY(this.fallbackRotation);
            Vector3f pos = new Vector3f();
            for (int i = 0; i < 4; i++) {
                quad.copyPos(i, pos);
                pos.add(-0.5f, -0.5f, -0.5f);
                pos.rotate(quaternion);
                pos.add(0.5f, 0.5f, 0.5f);
                quad.pos(i, pos);
            }
            return true;
        });
        this.pointer.emitItemQuads(stack, randomSupplier, context);
        context.popTransform();
    }

    // this is used in the block entity renderer
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        return base.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.base.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.base.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.base.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        /*
         * The entity is given when an item rendered on the hotbar, or when held in hand.
         */
        return new ItemOverrides() {
            @Override
            public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                if (level != null && entity instanceof LocalPlayer player) {
                    float offRads = (float) (player.getYRot() / 180.0f * (float) Math.PI + Math.PI);
                    MeteoriteCompassBakedModel.this.fallbackRotation = getAnimatedRotation(player.blockPosition(), true, offRads);
                } else {
                    MeteoriteCompassBakedModel.this.fallbackRotation = getAnimatedRotation(null, false, 0);
                }

                return originalModel;
            }
        };
    }

    /**
     * Gets the effective, animated rotation for the compass given the current position of the compass.
     */
    public static float getAnimatedRotation(@Nullable BlockPos pos, boolean prefetch, float playerRotation) {

        // Only query for a meteor position if we know our own position
        if (pos != null) {
            var ourChunkPos = new ChunkPos(pos);
            var closestMeteorite = CompassManager.INSTANCE.getClosestMeteorite(ourChunkPos, prefetch);

            // No close meteorite was found -> spin slowly
            if (closestMeteorite == null) {
                long timeMillis = System.currentTimeMillis();
                // .5 seconds per full rotation
                timeMillis %= 500;
                return timeMillis / 500.f * (float) Math.PI * 2;
            } else {
                var dx = pos.getX() - closestMeteorite.getX();
                var dz = pos.getZ() - closestMeteorite.getZ();
                var distanceSq = dx * dx + dz * dz;
                if (distanceSq > 6 * 6) {
                    var x = closestMeteorite.getX();
                    var z = closestMeteorite.getZ();
                    return (float) rad(pos.getX(), pos.getZ(), x, z) + playerRotation;
                }
            }
        }

        long timeMillis = System.currentTimeMillis();
        // 3 seconds per full rotation
        timeMillis %= 3000;
        return timeMillis / 3000.f * (float) Math.PI * 2;
    }

    private static double rad(double ax, double az, double bx, double bz) {
        var up = bz - az;
        var side = bx - ax;

        return Math.atan2(-up, side) - Math.PI / 2.0;
    }
}
