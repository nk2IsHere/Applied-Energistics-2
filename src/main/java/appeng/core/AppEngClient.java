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

import appeng.api.parts.CableRenderMode;
import appeng.client.EffectType;
import appeng.client.Hotkeys;
import appeng.client.commands.ClientCommands;
import appeng.client.gui.me.common.PendingCraftingJobs;
import appeng.client.gui.me.common.PinnedKeys;
import appeng.client.gui.style.StyleManager;
import appeng.client.guidebook.Guide;
import appeng.client.guidebook.PageAnchor;
import appeng.client.guidebook.command.GuidebookStructureCommands;
import appeng.client.guidebook.compiler.TagCompiler;
import appeng.client.guidebook.extensions.ConfigValueTagExtension;
import appeng.client.guidebook.hotkey.OpenGuideHotkey;
import appeng.client.guidebook.scene.ImplicitAnnotationStrategy;
import appeng.client.guidebook.scene.PartAnnotationStrategy;
import appeng.client.guidebook.screen.GlobalInMemoryHistory;
import appeng.client.guidebook.screen.GuideScreen;
import appeng.client.render.StorageCellClientTooltipComponent;
import appeng.client.render.effects.EnergyParticleData;
import appeng.client.render.effects.ParticleTypes;
import appeng.client.render.overlay.OverlayManager;
import appeng.core.definitions.AEBlocks;
import appeng.core.network.InitNetworkClient;
import appeng.core.network.serverbound.MouseWheelPacket;
import appeng.helpers.IMouseWheelItem;
import appeng.hooks.BlockAttackHook;
import appeng.hooks.ICustomPickBlock;
import appeng.hooks.MouseWheelScrolled;
import appeng.hooks.RenderBlockOutlineHook;
import appeng.init.client.*;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.siteexport.SiteExporter;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Client-specific functionality.
 */
@Environment(EnvType.CLIENT)
public class AppEngClient extends AppEngBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppEngClient.class);

    private static AppEngClient INSTANCE;

    /**
     * Last known cable render mode. Used to update all rendered blocks once at the end of the tick when the mode is
     * changed.
     */
    private CableRenderMode prevCableRenderMode = CableRenderMode.STANDARD;

    private final Guide guide;

    public AppEngClient() {
        this.registerParticleFactories();
        this.modelRegistryEvent();
        this.registerBlockColors();
        this.registerItemColors();
        this.registerEntityRenderers();
        this.registerEntityLayerDefinitions();
        this.registerClientTooltipComponents();
        this.registerClientCommands();

        InitNetworkClient.init();

        ClientPickBlockGatherCallback.EVENT.register(this::onPickBlock);
        ClientTickEvents.START_CLIENT_TICK.register(this::updateCableRenderMode);

        BlockAttackHook.install();
        RenderBlockOutlineHook.install();
        guide = createGuide();
        OpenGuideHotkey.init();

        ClientLifecycleEvents.CLIENT_STARTED.register(this::clientSetup);

        INSTANCE = this;
        notifyAddons("client");

        ClientTickEvents.END_CLIENT_TICK.register(c -> Hotkeys.checkHotkeys());

        ClientTickEvents.END_CLIENT_TICK.register(this::tickPinnedKeys);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            PendingCraftingJobs.clearPendingJobs();
            PinnedKeys.clearPinnedKeys();
        });

        registerTests();

        // Only activate the site exporter when we're not running a release version, since it'll
        // replace blocks around spawn.
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            SiteExporter.initialize();
        }
    }

    private void registerClientCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("ae2client");
            if (AEConfig.instance().isDebugToolsEnabled()) {
                for (var commandBuilder : ClientCommands.DEBUG_COMMANDS) {
                    commandBuilder.build(builder);
                }
            }
            dispatcher.register(builder);
        });
    }

    private Guide createGuide() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            var dispatcher = server.getCommands().getDispatcher();
            GuidebookStructureCommands.register(dispatcher);
        });

        return Guide.builder(MOD_ID, "ae2guide")
                .extension(ImplicitAnnotationStrategy.EXTENSION_POINT, new PartAnnotationStrategy())
                .extension(TagCompiler.EXTENSION_POINT, new ConfigValueTagExtension())
                .build();
    }

    private void tickPinnedKeys(Minecraft minecraft) {
        // Only prune pinned keys when no screen is currently open
        if (minecraft.screen == null) {
            PinnedKeys.prune();
        }
    }

    @Override
    public Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public void registerHotkey(String id) {
        Hotkeys.registerHotkey(id);
    }

    public static AppEngClient instance() {
        return Objects.requireNonNull(INSTANCE, "AppEngClient is not initialized");
    }

    public void registerParticleFactories() {
        InitParticleFactories.init();
    }

    public void registerBlockColors() {
        InitBlockColors.init(ColorProviderRegistry.BLOCK::register);
    }

    public void registerItemColors() {
        InitItemColors.init(ColorProviderRegistry.ITEM::register);
    }

    private void registerClientTooltipComponents() {
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof StorageCellTooltipComponent cellTooltipComponent) {
                return new StorageCellClientTooltipComponent(cellTooltipComponent);
            }
            return null;
        });
    }

    private void clientSetup(Minecraft client) {
        postClientSetup(client);

        MouseWheelScrolled.EVENT.register(this::wheelEvent);
        WorldRenderEvents.LAST.register(OverlayManager.getInstance()::renderWorldLastEvent);
    }

    private void registerEntityRenderers() {
        InitEntityRendering.init(EntityRendererRegistry::register);
    }

    private void registerEntityLayerDefinitions() {
        InitEntityLayerDefinitions.init((modelLayerLocation, layerDefinition) -> {
            EntityModelLayerRegistry.registerModelLayer(modelLayerLocation, () -> layerDefinition);
        });
    }

    /**
     * Called when other mods have finished initializing and the client is now available.
     */
    private void postClientSetup(Minecraft minecraft) {
        StyleManager.initialize(minecraft.getResourceManager());
        InitScreens.init();
        InitStackRenderHandlers.init();
    }

    @Environment(EnvType.CLIENT)
    public void modelRegistryEvent() {
        InitAdditionalModels.init();
        InitBlockEntityRenderers.init();
        InitItemModelsProperties.init();
        InitRenderTypes.init();
        InitBuiltInModels.init();
    }

    private boolean wheelEvent(double horizontalDelta, double verticalDelta) {
        if (verticalDelta == 0) {
            return false;
        }

        final Minecraft mc = Minecraft.getInstance();
        final Player player = mc.player;
        if (InteractionUtil.isInAlternateUseMode(player)) {
            final boolean mainHand = player.getItemInHand(InteractionHand.MAIN_HAND)
                    .getItem() instanceof IMouseWheelItem;
            final boolean offHand = player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof IMouseWheelItem;

            if (mainHand || offHand) {
                ClientPlayNetworking.send(new MouseWheelPacket(verticalDelta > 0));
                return true;
            }
        }

        return false;
    }

    public boolean shouldAddParticles(RandomSource r) {
        return switch (Minecraft.getInstance().options.particles().get()) {
            case ALL -> true;
            case DECREASED -> r.nextBoolean();
            case MINIMAL -> false;
        };
    }

    @Override
    public HitResult getCurrentMouseOver() {
        return Minecraft.getInstance().hitResult;
    }

    // FIXME: Instead of doing a custom packet and this dispatcher, we can use the
    // vanilla particle system
    @Override
    public void spawnEffect(EffectType effect, Level level, double posX, double posY,
            double posZ, Object o) {
        if (AEConfig.instance().isEnableEffects()) {
            switch (effect) {
                case Vibrant:
                    this.spawnVibrant(level, posX, posY, posZ);
                    return;
                case Energy:
                    this.spawnEnergy(level, posX, posY, posZ);
                    return;
                case Lightning:
                    this.spawnLightning(level, posX, posY, posZ);
                    return;
                default:
            }
        }
    }

    private void spawnVibrant(Level level, double x, double y, double z) {
        if (AppEngClient.instance().shouldAddParticles(level.getRandom())) {
            final double d0 = (level.getRandom().nextFloat() - 0.5F) * 0.26D;
            final double d1 = (level.getRandom().nextFloat() - 0.5F) * 0.26D;
            final double d2 = (level.getRandom().nextFloat() - 0.5F) * 0.26D;

            Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.VIBRANT, x + d0, y + d1, z + d2, 0.0D,
                    0.0D,
                    0.0D);
        }
    }

    private void spawnEnergy(Level level, double posX, double posY, double posZ) {
        var random = level.getRandom();
        final float x = (float) (Math.abs(random.nextInt()) % 100 * 0.01 - 0.5) * 0.7f;
        final float y = (float) (Math.abs(random.nextInt()) % 100 * 0.01 - 0.5) * 0.7f;
        final float z = (float) (Math.abs(random.nextInt()) % 100 * 0.01 - 0.5) * 0.7f;

        Minecraft.getInstance().particleEngine.createParticle(EnergyParticleData.FOR_BLOCK, posX + x, posY + y,
                posZ + z,
                -x * 0.1, -y * 0.1, -z * 0.1);
    }

    private void spawnLightning(Level level, double posX, double posY, double posZ) {
        Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.LIGHTNING, posX, posY + 0.3f, posZ, 0.0f,
                0.0f,
                0.0f);
    }

    private void updateCableRenderMode(Minecraft mc) {
        var currentMode = getCableRenderMode();

        // Handle changes to the cable-rendering mode
        if (currentMode == this.prevCableRenderMode) {
            return;
        }

        this.prevCableRenderMode = currentMode;

        if (mc.player == null || mc.level == null) {
            return;
        }

        // Invalidate all sections that contain a cable bus within view distance
        // This should asynchronously update the chunk meshes and as part of that use the new facade render mode
        var viewDistance = (int) Math.ceil(mc.levelRenderer.getLastViewDistance());
        ChunkPos.rangeClosed(mc.player.chunkPosition(), viewDistance).forEach(chunkPos -> {
            var chunk = mc.level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk != null) {
                for (var i = 0; i < chunk.getSectionsCount(); i++) {
                    var section = chunk.getSection(i);
                    if (section.maybeHas(state -> state.is(AEBlocks.CABLE_BUS.block()))) {
                        mc.levelRenderer.setSectionDirty(chunkPos.x, chunk.getSectionYFromSectionIndex(i), chunkPos.z);
                    }
                }
            }
        });
    }

    @Override
    public CableRenderMode getCableRenderMode() {
        if (Platform.isServer()) {
            return super.getCableRenderMode();
        }

        var mc = Minecraft.getInstance();
        if (mc.player == null) {
            return CableRenderMode.STANDARD;
        }

        return this.getCableRenderModeForPlayer(mc.player);
    }

    /**
     * Replaces a Forge-Hook that was done via a method in IForgeBlock.
     */
    private ItemStack onPickBlock(Player player, HitResult hitResult) {
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = player.level().getBlockState(blockPos);

            if (blockState.getBlock() instanceof ICustomPickBlock customPickBlock) {
                return customPickBlock.getPickBlock(blockState, hitResult, player.level(), blockPos, player);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void openGuideAtPreviousPage(ResourceLocation initialPage) {
        try {
            var screen = GuideScreen.openAtPreviousPage(guide, PageAnchor.page(initialPage),
                    GlobalInMemoryHistory.INSTANCE);

            openGuideScreen(screen);
        } catch (Exception e) {
            LOGGER.error("Failed to open guide.", e);
        }
    }

    @Override
    public void openGuideAtAnchor(PageAnchor anchor) {
        try {
            var screen = GuideScreen.openNew(guide, anchor, GlobalInMemoryHistory.INSTANCE);

            openGuideScreen(screen);
        } catch (Exception e) {
            LOGGER.error("Failed to open guide at {}.", anchor, e);
        }
    }

    private static void openGuideScreen(GuideScreen screen) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            screen.setReturnToOnClose(minecraft.screen);
        }

        minecraft.setScreen(screen);
    }

    public Guide getGuide() {
        return guide;
    }
}
