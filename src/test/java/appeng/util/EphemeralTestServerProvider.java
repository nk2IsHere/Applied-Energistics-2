//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package appeng.util;

import java.io.IOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.extension.*;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.Commands.CommandSelection;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;

public class EphemeralTestServerProvider implements ParameterResolver, Extension {
    public static final AtomicReference<MinecraftServer> SERVER = new AtomicReference();
    public static final AtomicBoolean IN_CONSTRUCTION = new AtomicBoolean();

    public EphemeralTestServerProvider() {
    }

    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == MinecraftServer.class;
    }

    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return grabServer();
    }

    public static MinecraftServer grabServer() {
        if (SERVER.get() != null) {
            return SERVER.get();
        } else {
            if (IN_CONSTRUCTION.compareAndSet(false, true)) {
                try {
                    Path tempDir = Files.createTempDirectory("test-mc-server-");
                    LevelStorageSource storage = LevelStorageSource.createDefault(tempDir.resolve("world"));
                    LevelStorageSource.LevelStorageAccess storageAccess = storage.validateAndCreateAccess("main");
                    PackRepository packrepository = ServerPacksSource.createPackRepository(storageAccess);
                    MinecraftServer server = MinecraftServer.spin((thread) -> EphemeralTestServerProvider.JUnitServer
                            .create(thread, tempDir, storageAccess, packrepository));
                    SERVER.set(server);
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        server.stopServer();
                        LogManager.shutdown();
                    }));
                } catch (Exception ex) {
                    LogUtils.getLogger().error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", ex);
                    throw new RuntimeException(ex);
                }
            }

            while (SERVER.get() == null) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            return SERVER.get();
        }
    }

    public static class JUnitServer extends MinecraftServer {
        private static final Logger LOGGER = LogUtils.getLogger();
        private static final Services NO_SERVICES;
        private static final GameRules TEST_GAME_RULES;
        private static final WorldOptions WORLD_OPTIONS;
        private final Path tempDir;
        private final LocalSampleLogger sampleLogger = new LocalSampleLogger(1);

        public static JUnitServer create(Thread thread, Path tempDir, LevelStorageSource.LevelStorageAccess access,
                PackRepository resources) {
            resources.reload();
            WorldDataConfiguration config = new WorldDataConfiguration(
                    new DataPackConfig(new ArrayList(resources.getAvailableIds()), List.of()),
                    FeatureFlags.REGISTRY.allFlags());
            LevelSettings levelsettings = new LevelSettings("Test Level", GameType.CREATIVE, false, Difficulty.NORMAL,
                    true, TEST_GAME_RULES, config);
            WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(resources, config, false, true);
            WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(worldloader$packconfig,
                    CommandSelection.DEDICATED, 4);

            try {
                LOGGER.debug("Starting resource loading");
                Stopwatch stopwatch = Stopwatch.createStarted();
                WorldStem worldstem = (WorldStem) Util
                        .blockUntilDone((exec) -> WorldLoader.load(worldloader$initconfig, (ctx) -> {
                            Registry<LevelStem> registry = (new MappedRegistry(Registries.LEVEL_STEM,
                                    Lifecycle.stable())).freeze();
                            WorldDimensions.Complete worlddimensions$complete = ((WorldPreset) ctx.datapackWorldgen()
                                    .registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                                    .value()).createWorldDimensions().bake(registry);
                            return new WorldLoader.DataLoadOutput(
                                    new PrimaryLevelData(levelsettings, WORLD_OPTIONS,
                                            worlddimensions$complete.specialWorldProperty(),
                                            worlddimensions$complete.lifecycle()),
                                    worlddimensions$complete.dimensionsRegistryAccess());
                        }, WorldStem::new, Util.backgroundExecutor(), exec)).get();
                stopwatch.stop();
                LOGGER.debug("Finished resource loading after {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
                return new JUnitServer(thread, access, resources, worldstem, tempDir);
            } catch (Exception exception) {
                LOGGER.warn("Failed to load vanilla datapack, bit oops", exception);
                System.exit(-1);
                throw new IllegalStateException();
            }
        }

        public JUnitServer(Thread thread, LevelStorageSource.LevelStorageAccess access, PackRepository pack,
                WorldStem stem, Path tempDir) {
            super(thread, access, pack, stem, Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES,
                    LoggerChunkProgressListener::createFromGameruleRadius);
            this.tempDir = tempDir;
        }

        public boolean initServer() {
            this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, 1) {
            });
            ServerLifecycleEvents.SERVER_STARTING.invoker().onServerStarting(this);
            LOGGER.info("Started ephemeral JUnit server");
            ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(this);
            return true;
        }

        public void tickServer(BooleanSupplier sup) {
            super.tickServer(sup);
            EphemeralTestServerProvider.SERVER.set(this);
        }

        public boolean saveEverything(boolean p_195515_, boolean p_195516_, boolean p_195517_) {
            return false;
        }

        public void stopServer() {
            LOGGER.info("Stopping server");
            ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(this);
            this.getConnection().stop();
            this.getPlayerList().removeAll();

            try {
                this.storageSource.deleteLevel();
                this.storageSource.close();
                Files.delete(this.tempDir);
                ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(this);
            } catch (IOException ioexception) {
                LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelId(), ioexception);
            }

        }

        public void waitUntilNextTick() {
            this.runAllTasks();
        }

        public SystemReport fillServerSystemReport(SystemReport report) {
            report.setDetail("Type", "Test ephemeral server");
            return report;
        }

        public boolean isHardcore() {
            return false;
        }

        public int getOperatorUserPermissionLevel() {
            return 0;
        }

        public int getFunctionCompilationLevel() {
            return 4;
        }

        public boolean shouldRconBroadcast() {
            return false;
        }

        public boolean isDedicatedServer() {
            return false;
        }

        public int getRateLimitPacketsPerSecond() {
            return 0;
        }

        public boolean isEpollEnabled() {
            return false;
        }

        public boolean isCommandBlockEnabled() {
            return true;
        }

        public boolean isPublished() {
            return false;
        }

        public boolean shouldInformAdmins() {
            return false;
        }

        public boolean isSingleplayerOwner(GameProfile profile) {
            return false;
        }

        protected SampleLogger getTickTimeLogger() {
            return this.sampleLogger;
        }

        public boolean isTickTimeLoggingEnabled() {
            return false;
        }

        static {
            NO_SERVICES = new Services((MinecraftSessionService) null, ServicesKeySet.EMPTY,
                    (GameProfileRepository) null, (GameProfileCache) null);
            TEST_GAME_RULES = (GameRules) Util.make(new GameRules(), (rules) -> {
                ((GameRules.BooleanValue) rules.getRule(GameRules.RULE_DOMOBSPAWNING)).set(false,
                        (MinecraftServer) null);
                ((GameRules.BooleanValue) rules.getRule(GameRules.RULE_WEATHER_CYCLE)).set(false,
                        (MinecraftServer) null);
            });
            WORLD_OPTIONS = new WorldOptions(0L, false, false);
        }
    }
}
