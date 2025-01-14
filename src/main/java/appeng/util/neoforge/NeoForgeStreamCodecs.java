/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package appeng.util.neoforge;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import appeng.util.neoforge.util.Lazy;

public final class NeoForgeStreamCodecs {
    public static final StreamCodec<FriendlyByteBuf, byte[]> UNBOUNDED_BYTE_ARRAY = new StreamCodec<>() {
        public byte[] decode(FriendlyByteBuf buf) {
            return buf.readByteArray();
        }

        public void encode(FriendlyByteBuf buf, byte[] data) {
            buf.writeByteArray(data);
        }
    };

    public static final StreamCodec<FriendlyByteBuf, ChunkPos> CHUNK_POS = new StreamCodec<>() {
        @Override
        public ChunkPos decode(FriendlyByteBuf buf) {
            return buf.readChunkPos();
        }

        @Override
        public void encode(FriendlyByteBuf buf, ChunkPos pos) {
            buf.writeChunkPos(pos);
        }
    };

    public static <B, V> StreamCodec<B, V> lazy(Supplier<StreamCodec<B, V>> streamCodecSupplier) {
        return new LazyStreamCodec<>(streamCodecSupplier);
    }

    private static class LazyStreamCodec<B, V> implements StreamCodec<B, V> {
        private final Lazy<StreamCodec<B, V>> delegate;

        public LazyStreamCodec(Supplier<StreamCodec<B, V>> streamCodecSupplier) {
            delegate = Lazy.of(streamCodecSupplier);
        }

        @Override
        public void encode(B buf, V value) {
            delegate.get().encode(buf, value);
        }

        @Override
        public V decode(B buf) {
            return delegate.get().decode(buf);
        }
    }

    public static <B extends FriendlyByteBuf, V extends Enum<V>> StreamCodec<B, V> enumCodec(Class<V> enumClass) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return buf.readEnum(enumClass);
            }

            @Override
            public void encode(B buf, V value) {
                buf.writeEnum(value);
            }
        };
    }

    /**
     * Creates a stream codec to encode and decode a {@link ResourceKey} that identifies a registry.
     */
    public static <B extends FriendlyByteBuf> StreamCodec<B, ResourceKey<? extends Registry<?>>> registryKey() {
        return new StreamCodec<>() {
            @Override
            public ResourceKey<? extends Registry<?>> decode(B buf) {
                return ResourceKey.createRegistryKey(buf.readResourceLocation());
            }

            @Override
            public void encode(B buf, ResourceKey<? extends Registry<?>> value) {
                buf.writeResourceLocation(value.location());
            }
        };
    }

    /**
     * Similar to {@link StreamCodec#unit(Object)}, but without checks for the value to be encoded.
     */
    public static <B, V> StreamCodec<B, V> uncheckedUnit(final V defaultValue) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return defaultValue;
            }

            @Override
            public void encode(B buf, V value) {}
        };
    }
}