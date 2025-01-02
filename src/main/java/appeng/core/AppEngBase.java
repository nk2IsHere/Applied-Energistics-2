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

package appeng.core;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import appeng.api.ids.AEComponents;
import appeng.core.definitions.*;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.InitNetwork;
import appeng.core.network.TargetPoint;
import appeng.init.*;
import appeng.init.client.InitParticleTypes;
import appeng.init.internal.*;
import appeng.recipes.AERecipeSerializers;
import appeng.recipes.AERecipeTypes;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;

import appeng.api.IAEAddonEntrypoint;
import appeng.api.parts.CableRenderMode;
import appeng.hooks.ToolItemHook;
import appeng.hooks.WrenchHook;
import appeng.hooks.ticking.TickHandler;
import appeng.hotkeys.HotkeyActions;
import appeng.init.client.InitKeyTypes;
import appeng.init.worldgen.InitStructures;
import appeng.server.AECommand;
import appeng.server.services.ChunkLoadingService;
import appeng.server.testworld.GameTestPlotAdapter;
import appeng.sounds.AppEngSounds;
import appeng.spatial.SpatialStorageChunkGenerator;
import appeng.spatial.SpatialStorageDimensionIds;

/**
 * Mod functionality that is common to both dedicated server and client.
 * <p>
 * Note that a client will still have zero or more embedded servers (although only one at a time).
 */
public abstract class AppEngBase implements AppEng {

    /**
     * While we process a player-specific part placement/cable interaction packet, we need to use that player's
     * transparent-facade mode to understand whether the player can see through facades or not.
     * <p>
     * We need to use this method since the collision shape methods do not know about the player that the shape is being
     * requested for, so they will call {@link #getCableRenderMode()} below, which then will use this field to figure
     * out which player it's for.
     */
    private final ThreadLocal<Player> partInteractionPlayer = new ThreadLocal<>();

    static AppEngBase INSTANCE;

    private MinecraftServer currentServer;

    public AppEngBase() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;

        if (AEConfig.instance() == null) {
            AEConfig.load(FabricLoader.getInstance().getConfigDir());
        }

        InitKeyTypes.init();

        // Initialize items in order
        AEParts.init();
        AEBlocks.DR.register();
        AEItems.DR.register();
        AEBlockEntities.DR.register();
        AEComponents.DR.register();
        AEEntities.DR.register();
        AERecipeTypes.DR.register();
        AERecipeSerializers.DR.register();
        InitStructures.register();

        registerSounds(BuiltInRegistries.SOUND_EVENT);
        registerCreativeTabs(BuiltInRegistries.CREATIVE_MODE_TAB);
        InitParticleTypes.init(BuiltInRegistries.PARTICLE_TYPE);
        InitMenuTypes.init(BuiltInRegistries.MENU);
        registerDimension();
        InitVillager.init(BuiltInRegistries.VILLAGER_PROFESSION);

        // Now that item instances are available, we can initialize registries that need item instances
        InitGridLinkables.init();
        InitStorageCells.init();

        postRegistrationInitialization();

        TickHandler.instance().init();
        InitNetwork.init();

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerAboutToStart);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::serverStopped);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::serverStopping);
        ServerLifecycleEvents.SERVER_STARTING.register(this::registerCommands);

        UseBlockCallback.EVENT.register(WrenchHook::onPlayerUseBlock);
        UseBlockCallback.EVENT.register(ToolItemHook::onPlayerUseBlock);

        HotkeyActions.init();
    }

    /**
     * Runs after all mods have had time to run their registrations into registries.
     */
    public void postRegistrationInitialization() {
        // This has to be here because it relies on caps and god knows when those are available...
        InitP2PAttunements.init();

        InitApiLookup.init();
        InitCauldronInteraction.init();
        InitDispenserBehavior.init();

        AEConfig.instance().save();
        InitUpgrades.init();
    }

    public void registerCommands(MinecraftServer server) {
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        new AECommand().register(dispatcher);
    }

    public void registerSounds(Registry<SoundEvent> registry) {
        AppEngSounds.register(registry);
    }

    public void registerDimension() {
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, SpatialStorageDimensionIds.CHUNK_GENERATOR_ID,
                SpatialStorageChunkGenerator.CODEC);
    }

    public void registerCreativeTabs(Registry<CreativeModeTab> registry) {
        MainCreativeTab.init(registry);
        FacadeCreativeTab.init(registry);
    }

    private void onServerAboutToStart(MinecraftServer server) {
        this.currentServer = server;
        ChunkLoadingService.getInstance().onServerAboutToStart();
    }

    private void serverStopping(MinecraftServer server) {
        ChunkLoadingService.getInstance().onServerStopping();
    }

    private void serverStopped(MinecraftServer server) {
        TickHandler.instance().shutdown();
        if (this.currentServer == server) {
            this.currentServer = null;
        }
    }

    @Override
    public Collection<ServerPlayer> getPlayers() {
        var server = getCurrentServer();

        if (server != null) {
            return server.getPlayerList().getPlayers();
        }

        return Collections.emptyList();
    }

    @Override
    public void sendToAllNearExcept(Player p, double x, double y, double z,
            double dist, Level level, ClientboundPacket packet) {
        if (level.isClientSide()) {
            return;
        }
        for (ServerPlayer o : getPlayers()) {
            try (var otherPlayerLevel = o.level()) {
                if (o != p && otherPlayerLevel == level) {
                    final double dX = x - o.getX();
                    final double dY = y - o.getY();
                    final double dZ = z - o.getZ();
                    if (dX * dX + dY * dY + dZ * dZ < dist * dist) {
                        ServerPlayNetworking.send(o, packet);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void sendToAllAround(ClientboundPacket message, TargetPoint point) {
        PlayerLookup
            .around((ServerLevel) point.level, new Vec3(point.x, point.y, point.z), point.radius)
            .forEach(player -> {
                if (player != point.excluded) {
                    ServerPlayNetworking.send(player, message);
                }
            });
    }

    @Override
    public void setPartInteractionPlayer(Player player) {
        this.partInteractionPlayer.set(player);
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        return this.getCableRenderModeForPlayer(partInteractionPlayer.get());
    }

    @Nullable
    @Override
    public MinecraftServer getCurrentServer() {
        return currentServer;
    }

    protected final CableRenderMode getCableRenderModeForPlayer(@Nullable Player player) {
        if (player != null) {
            if (AEItems.NETWORK_TOOL.is(player.getItemInHand(InteractionHand.MAIN_HAND))
                    || AEItems.NETWORK_TOOL.is(player.getItemInHand(InteractionHand.OFF_HAND))) {
                return CableRenderMode.CABLE_VIEW;
            }
        }

        return CableRenderMode.STANDARD;
    }

    protected final void notifyAddons(String sideSpecificEntrypoint) {
        var entrypoints = FabricLoader.getInstance().getEntrypoints(AppEng.MOD_ID, IAEAddonEntrypoint.class);
        for (var entrypoint : entrypoints) {
            entrypoint.onAe2Initialized();
        }

        var sideSpecificEntrypoints = FabricLoader.getInstance()
                .getEntrypoints(AppEng.MOD_ID + ":" + sideSpecificEntrypoint, IAEAddonEntrypoint.class);
        for (var entrypoint : sideSpecificEntrypoints) {
            entrypoint.onAe2Initialized();
        }
    }

    protected static void registerTests() {
        if ("true".equals(System.getProperty("appeng.tests"))) {
            GameTestRegistry.register(GameTestPlotAdapter.class);
        }
    }
}
