/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package appeng.util.neoforge.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.RegistryPatchGenerator;

/**
 * An extension of the {@link RegistriesDatapackGenerator} which properly handles
 * referencing existing dynamic registry objects within another dynamic registry
 * object.
 */
public class DatapackBuiltinEntriesProvider extends RegistriesDatapackGenerator {
    private final CompletableFuture<HolderLookup.Provider> fullRegistries;

    /**
     * Constructs a new datapack provider which generates all registry objects
     * from the provided mods using the holder.
     *
     * @param output     the target directory of the data generator
     * @param registries a future of a lookup for registries and their objects
     */
    public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries) {
        super(output, registries.thenApply(RegistrySetBuilder.PatchedRegistries::patches));
        this.fullRegistries = registries.thenApply(RegistrySetBuilder.PatchedRegistries::full);
    }

    public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, RegistrySetBuilder datapackEntriesBuilder) {
        this(output, RegistryPatchGenerator.createLookup(registries, datapackEntriesBuilder));
    }

    /**
     * Get the registry holder lookup provider that includes elements added from the {@link RegistrySetBuilder}
     */
    public CompletableFuture<HolderLookup.Provider> getRegistryProvider() {
        return fullRegistries;
    }
}
