/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import appeng.api.config.CondenserOutput;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnit;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.networking.pathing.ChannelMode;
import appeng.core.config.BooleanOption;
import appeng.core.config.ConfigFileManager;
import appeng.core.config.ConfigSection;
import appeng.core.config.ConfigValidationException;
import appeng.core.config.DoubleOption;
import appeng.core.config.EnumOption;
import appeng.core.config.IntegerOption;
import appeng.core.settings.TickRates;
import appeng.util.EnumCycler;
import appeng.util.Platform;

public final class AEConfig {

    public static final String CLIENT_CONFIG_PATH = "ae2/client.json";
    public static final String COMMON_CONFIG_PATH = "ae2/common.json";
    public final ClientConfig CLIENT;
    public final ConfigFileManager clientConfigManager;
    public final CommonConfig COMMON;
    public final ConfigFileManager commonConfigManager;

    AEConfig(Path configDir) {
        ConfigSection clientRoot = ConfigSection.createRoot();
        CLIENT = new ClientConfig(clientRoot);
        clientConfigManager = createConfigFileManager(clientRoot, configDir, CLIENT_CONFIG_PATH);

        ConfigSection commonRoot = ConfigSection.createRoot();
        COMMON = new CommonConfig(commonRoot);
        commonConfigManager = createConfigFileManager(commonRoot, configDir, COMMON_CONFIG_PATH);

        syncClientConfig();
        syncCommonConfig();
    }

    private static ConfigFileManager createConfigFileManager(ConfigSection commonRoot, Path configDir,
            String filename) {
        var configFile = configDir.resolve(filename);
        ConfigFileManager result = new ConfigFileManager(commonRoot, configFile);
        if (!Files.exists(configFile)) {
            result.save(); // Save a default file
        } else {
            try {
                result.load();
            } catch (ConfigValidationException e) {
                AELog.error("Failed to load AE2 Config. Making backup", e);

                // Backup and delete config files to reset them
                makeBackupAndReset(configDir, filename);
            }

            // Re-save immediately to write-out new defaults
            try {
                result.save();
            } catch (Exception e) {
                AELog.warn(e);
            }
        }
        return result;
    }

    // Default Energy Conversion Rates
    private static final double DEFAULT_FE_EXCHANGE = 2.0;

    // Config instance
    private static AEConfig instance;

    public static void load(Path configFolder) {
        if (instance != null) {
            throw new IllegalStateException("Config is already loaded");
        }
        instance = new AEConfig(configFolder);
    }

    private static void makeBackupAndReset(Path configFolder, String configFile) {
        var backupFile = configFolder.resolve(configFile + ".bak");
        var originalFile = configFolder.resolve(configFile);
        try {
            Files.move(originalFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            AELog.warn("Failed to backup config file %s: %s!", originalFile, e);
        }
    }

    // Misc
    private int formationPlaneEntityLimit;
    private boolean enableEffects;
    private boolean useLargeFonts;
    private boolean useColoredCraftingStatus;
    private boolean disableColoredCableRecipesInRecipeViewer;
    private boolean isEnableFacadesInRecipeViewer;
    private boolean isEnableFacadeRecipesInRecipeViewer;
    private int craftingCalculationTimePerTick;
    private boolean spatialAnchorEnablesRandomTicks;

    // Spatial IO/Dimension
    private double spatialPowerExponent;
    private double spatialPowerMultiplier;

    // Batteries
    private int wirelessTerminalBattery;
    private int entropyManipulatorBattery;
    private int matterCannonBattery;
    private int portableCellBattery;
    private int colorApplicatorBattery;
    private int chargedStaffBattery;

    // Wireless
    private double wirelessBaseCost;
    private double wirelessCostMultiplier;
    private double wirelessTerminalDrainMultiplier;
    private double wirelessBaseRange;
    private double wirelessBoosterRangeMultiplier;
    private double wirelessBoosterExp;
    private double wirelessHighWirelessCount;

    // Tunnels
    public static final double TUNNEL_POWER_LOSS = 0.05;

    private void syncClientConfig() {
        this.disableColoredCableRecipesInRecipeViewer = CLIENT.disableColoredCableRecipesInRecipeViewer.get();
        this.isEnableFacadesInRecipeViewer = CLIENT.enableFacadesInRecipeViewer.get();
        this.isEnableFacadeRecipesInRecipeViewer = CLIENT.enableFacadeRecipesInRecipeViewer.get();
        this.enableEffects = CLIENT.enableEffects.get();
        this.useLargeFonts = CLIENT.useLargeFonts.get();
        this.useColoredCraftingStatus = CLIENT.useColoredCraftingStatus.get();
    }

    private void syncCommonConfig() {
        PowerUnit.FE.conversionRatio = COMMON.powerRatioForgeEnergy.get();
        PowerMultiplier.CONFIG.multiplier = COMMON.powerUsageMultiplier.get();

        CondenserOutput.MATTER_BALLS.requiredPower = COMMON.condenserMatterBallsPower.get();
        CondenserOutput.SINGULARITY.requiredPower = COMMON.condenserSingularityPower.get();

        this.wirelessBaseCost = COMMON.wirelessBaseCost.get();
        this.wirelessCostMultiplier = COMMON.wirelessCostMultiplier.get();
        this.wirelessBaseRange = COMMON.wirelessBaseRange.get();
        this.wirelessBoosterRangeMultiplier = COMMON.wirelessBoosterRangeMultiplier.get();
        this.wirelessBoosterExp = COMMON.wirelessBoosterExp.get();
        this.wirelessHighWirelessCount = COMMON.wirelessHighWirelessCount.get();
        this.wirelessTerminalDrainMultiplier = COMMON.wirelessTerminalDrainMultiplier.get();

        this.formationPlaneEntityLimit = COMMON.formationPlaneEntityLimit.get();

        this.wirelessTerminalBattery = COMMON.wirelessTerminalBattery.get();
        this.chargedStaffBattery = COMMON.chargedStaffBattery.get();
        this.entropyManipulatorBattery = COMMON.entropyManipulatorBattery.get();
        this.portableCellBattery = COMMON.portableCellBattery.get();
        this.colorApplicatorBattery = COMMON.colorApplicatorBattery.get();
        this.matterCannonBattery = COMMON.matterCannonBattery.get();

        for (TickRates tr : TickRates.values()) {
            tr.setMin(COMMON.tickRateMin.get(tr).get());
            tr.setMax(COMMON.tickRateMax.get(tr).get());
        }

        this.spatialPowerMultiplier = COMMON.spatialPowerMultiplier.get();
        this.spatialPowerExponent = COMMON.spatialPowerExponent.get();

        this.craftingCalculationTimePerTick = COMMON.craftingCalculationTimePerTick.get();
        this.spatialAnchorEnablesRandomTicks = COMMON.spatialAnchorEnableRandomTicks.get();

        AELog.setCraftingLogEnabled(COMMON.craftingLog.get());
        AELog.setDebugLogEnabled(COMMON.debugLog.get());
        AELog.setGridLogEnabled(COMMON.gridLog.get());
    }

    public static AEConfig instance() {
        return instance;
    }

    // Tunnels
    public double getP2PTunnelEnergyTax() {
        return COMMON.p2pTunnelEnergyTax.get();
    }

    public double getP2PTunnelTransportTax() {
        return COMMON.p2pTunnelTransportTax.get();
    }

    public double wireless_getDrainRate(double range) {
        return COMMON.wirelessTerminalDrainMultiplier.get() * range;
    }

    public double wireless_getMaxRange(int boosters) {
        return COMMON.wirelessBaseRange.get()
            + COMMON.wirelessBoosterRangeMultiplier.get() * Math.pow(boosters, COMMON.wirelessBoosterExp.get());
    }

    public double wireless_getPowerDrain(int boosters) {
        return COMMON.wirelessBaseCost.get()
            + COMMON.wirelessCostMultiplier.get()
            * Math.pow(boosters, 1 + boosters / COMMON.wirelessHighWirelessCount.get());
    }

    public boolean isSearchModNameInTooltips() {
        return CLIENT.searchModNameInTooltips.get();
    }

    public void setSearchModNameInTooltips(boolean enable) {
        CLIENT.searchModNameInTooltips.set(enable);
    }

    public boolean isUseExternalSearch() {
        return CLIENT.useExternalSearch.get();
    }

    public void setUseExternalSearch(boolean enable) {
        CLIENT.useExternalSearch.set(enable);
    }

    public boolean isClearExternalSearchOnOpen() {
        return CLIENT.clearExternalSearchOnOpen.get();
    }

    public void setClearExternalSearchOnOpen(boolean enable) {
        CLIENT.clearExternalSearchOnOpen.set(enable);
    }

    public boolean isRememberLastSearch() {
        return CLIENT.rememberLastSearch.get();
    }

    public void setRememberLastSearch(boolean enable) {
        CLIENT.rememberLastSearch.set(enable);
    }

    public boolean isAutoFocusSearch() {
        return CLIENT.autoFocusSearch.get();
    }

    public void setAutoFocusSearch(boolean enable) {
        CLIENT.autoFocusSearch.set(enable);
    }

    public boolean isSyncWithExternalSearch() {
        return CLIENT.syncWithExternalSearch.get();
    }

    public void setSyncWithExternalSearch(boolean enable) {
        CLIENT.syncWithExternalSearch.set(enable);
    }

    public TerminalStyle getTerminalStyle() {
        return CLIENT.terminalStyle.get();
    }

    public void setTerminalStyle(TerminalStyle setting) {
        CLIENT.terminalStyle.set(setting);
    }

    public double getGridEnergyStoragePerNode() {
        return COMMON.gridEnergyStoragePerNode.get();
    }

    public double getCrystalResonanceGeneratorRate() {
        return COMMON.crystalResonanceGeneratorRate.get();
    }

    public PowerUnit getSelectedEnergyUnit() {
        return this.CLIENT.selectedPowerUnit.get();
    }

    public void nextEnergyUnit(boolean backwards) {
        var selected = EnumCycler.rotateEnum(getSelectedEnergyUnit(), backwards,
            Settings.POWER_UNITS.getValues());
        CLIENT.selectedPowerUnit.set(selected);
    }

    // Getters

    public boolean isDebugToolsEnabled() {
        return COMMON.debugTools.get();
    }

    public int getFormationPlaneEntityLimit() {
        return this.formationPlaneEntityLimit;
    }

    public boolean isEnableEffects() {
        return this.enableEffects;
    }

    public boolean isUseLargeFonts() {
        return this.useLargeFonts;
    }

    public boolean isUseColoredCraftingStatus() {
        return this.useColoredCraftingStatus;
    }

    public boolean isDisableColoredCableRecipesInRecipeViewer() {
        return CLIENT.disableColoredCableRecipesInRecipeViewer.get();
    }

    public boolean isEnableFacadesInRecipeViewer() {
        return CLIENT.enableFacadesInRecipeViewer.get();
    }

    public boolean isEnableFacadeRecipesInRecipeViewer() {
        return CLIENT.enableFacadeRecipesInRecipeViewer.get();
    }

    public boolean isExposeNetworkInventoryToEmi() {
        return CLIENT.exposeNetworkInventoryToEmi.get();
    }

    public boolean isSpatialAnchorEnablesRandomTicks() {
        return COMMON.spatialAnchorEnableRandomTicks.get();
    }

    public double getSpatialPowerExponent() {
        return COMMON.spatialPowerExponent.get();
    }

    public double getSpatialPowerMultiplier() {
        return COMMON.spatialPowerMultiplier.get();
    }

    public double getChargerChargeRate() {
        return COMMON.chargerChargeRate.get();
    }

    public DoubleSupplier getWirelessTerminalBattery() {
        return COMMON.wirelessTerminalBattery::get;
    }

    public DoubleSupplier getEntropyManipulatorBattery() {
        return COMMON.entropyManipulatorBattery::get;
    }

    public DoubleSupplier getMatterCannonBattery() {
        return COMMON.matterCannonBattery::get;
    }

    public DoubleSupplier getPortableCellBattery() {
        return COMMON.portableCellBattery::get;
    }

    public DoubleSupplier getColorApplicatorBattery() {
        return COMMON.colorApplicatorBattery::get;
    }

    public DoubleSupplier getChargedStaffBattery() {
        return COMMON.chargedStaffBattery::get;
    }

    public boolean isShowDebugGuiOverlays() {
        return CLIENT.debugGuiOverlays.get();
    }

    public void setShowDebugGuiOverlays(boolean enable) {
        CLIENT.debugGuiOverlays.set(enable);
    }

    public boolean isSpawnPressesInMeteoritesEnabled() {
        return COMMON.spawnPressesInMeteorites.get();
    }

    public boolean isSpawnFlawlessOnlyEnabled() {
        return COMMON.spawnFlawlessOnly.get();
    }

    public boolean isMatterCanonBlockDamageEnabled() {
        return COMMON.matterCannonBlockDamage.get();
    }

    public boolean isTinyTntBlockDamageEnabled() {
        return COMMON.tinyTntBlockDamage.get();
    }

    public int getGrowthAcceleratorSpeed() {
        return COMMON.growthAcceleratorSpeed.get();
    }

    public boolean isBlockUpdateLogEnabled() {
        return COMMON.blockUpdateLog.get();
    }

    public boolean isChunkLoggerTraceEnabled() {
        return COMMON.chunkLoggerTrace.get();
    }

    public ChannelMode getChannelMode() {
        return COMMON.channels.get();
    }

    public void setChannelModel(ChannelMode mode) {
        COMMON.channels.set(mode);
    }

    /**
     * @return True if an in-world preview of parts and facade placement should be shown when holding one in hand.
     */
    public boolean isPlacementPreviewEnabled() {
        return CLIENT.showPlacementPreview.get();
    }

    // Tooltip settings

    /**
     * Show upgrade inventory in tooltips of storage cells and similar devices.
     */
    public boolean isTooltipShowCellUpgrades() {
        return CLIENT.tooltipShowCellUpgrades.get();
    }

    /**
     * Show part of the content in tooltips of storage cells and similar devices.
     */
    public boolean isTooltipShowCellContent() {
        return CLIENT.tooltipShowCellContent.get();
    }

    /**
     * How much of the content to show in storage cellls and similar devices.
     */
    public int getTooltipMaxCellContentShown() {
        return CLIENT.tooltipMaxCellContentShown.get();
    }

    public boolean isPinAutoCraftedItems() {
        return CLIENT.pinAutoCraftedItems.get();
    }

    public void setPinAutoCraftedItems(boolean enabled) {
        CLIENT.pinAutoCraftedItems.set(enabled);
    }

    public boolean isNotifyForFinishedCraftingJobs() {
        return CLIENT.notifyForFinishedCraftingJobs.get();
    }

    public void setNotifyForFinishedCraftingJobs(boolean enabled) {
        CLIENT.notifyForFinishedCraftingJobs.set(enabled);
    }

    public boolean isClearGridOnClose() {
        return CLIENT.clearGridOnClose.get();
    }

    public void setClearGridOnClose(boolean enabled) {
        CLIENT.clearGridOnClose.set(enabled);
    }

    public double getVibrationChamberBaseEnergyPerFuelTick() {
        return COMMON.vibrationChamberBaseEnergyPerFuelTick.get();
    }

    public int getVibrationChamberMinEnergyPerGameTick() {
        return COMMON.vibrationChamberMinEnergyPerTick.get();
    }

    public int getVibrationChamberMaxEnergyPerGameTick() {
        return COMMON.vibrationChamberMaxEnergyPerTick.get();
    }

    public int getTerminalMargin() {
        return CLIENT.terminalMargin.get();
    }

    public int getCraftingCalculationTimePerTick() {
        return COMMON.craftingCalculationTimePerTick.get();
    }

    public boolean isAnnihilationPlaneSkyDustGenerationEnabled() {
        return COMMON.annihilationPlaneSkyDustGeneration.get();
    }

    public void save() {
    }

    public void reload() {
        clientConfigManager.load();
        commonConfigManager.load();

        syncClientConfig();
        syncCommonConfig();
    }
    // Setters keep visibility as low as possible.

    private static class ClientConfig {

        // Misc
        public final BooleanOption enableEffects;
        public final BooleanOption useLargeFonts;
        public final BooleanOption useColoredCraftingStatus;
        public final BooleanOption disableColoredCableRecipesInRecipeViewer;
        public final BooleanOption enableFacadesInRecipeViewer;
        public final BooleanOption enableFacadeRecipesInRecipeViewer;
        public final BooleanOption exposeNetworkInventoryToEmi;
        public final EnumOption<PowerUnit> selectedPowerUnit;
        public final BooleanOption debugGuiOverlays;
        public final BooleanOption showPlacementPreview;
        public final BooleanOption notifyForFinishedCraftingJobs;

        // Terminal Settings
        public final EnumOption<TerminalStyle> terminalStyle;
        public final BooleanOption pinAutoCraftedItems;
        public final BooleanOption clearGridOnClose;
        public final IntegerOption terminalMargin;

        // Search Settings
        public final BooleanOption searchModNameInTooltips;
        public final BooleanOption useExternalSearch;
        public final BooleanOption clearExternalSearchOnOpen;
        public final BooleanOption syncWithExternalSearch;
        public final BooleanOption rememberLastSearch;
        public final BooleanOption autoFocusSearch;

        // Tooltip settings
        public final BooleanOption tooltipShowCellUpgrades;
        public final BooleanOption tooltipShowCellContent;
        public final IntegerOption tooltipMaxCellContentShown;

        public ClientConfig(ConfigSection root) {
            var client = root.subsection("client");
            this.disableColoredCableRecipesInRecipeViewer = client.addBoolean("disableColoredCableRecipesInRecipeViewer", true);
            this.enableFacadeRecipesInRecipeViewer = client.addBoolean("enableFacadeRecipesInRecipeViewer", true,
                    "Show facades in JEI ingredient list");
            this.enableFacadesInRecipeViewer = client.addBoolean("enableFacadesInRecipeViewer", true,
                    "Show facade recipes in JEI for supported blocks");
            this.exposeNetworkInventoryToEmi = client.addBoolean("exposeNetworkInventoryToEmi", false,
                "Expose the full network inventory to EMI, which might cause performance problems.");
            this.enableEffects = client.addBoolean("enableEffects", true);
            this.useLargeFonts = client.addBoolean("useTerminalUseLargeFont", false);
            this.useColoredCraftingStatus = client.addBoolean("useColoredCraftingStatus", true);
            this.selectedPowerUnit = client.addEnum("PowerUnit", PowerUnit.AE, "Power unit shown in AE UIs");
            this.debugGuiOverlays = client.addBoolean("showDebugGuiOverlays", false, "Show debugging GUI overlays");
            this.showPlacementPreview = client.addBoolean("showPlacementPreview", true,
                    "Show a preview of part and facade placement");
            this.notifyForFinishedCraftingJobs = client.addBoolean("notifyForFinishedCraftingJobs", true,
                    "Show toast when long-running crafting jobs finish.");

            var terminals = root.subsection("terminals");
            this.terminalStyle = terminals.addEnum("terminalStyle", TerminalStyle.SMALL);
            this.pinAutoCraftedItems = terminals.addBoolean("pinAutoCraftedItems", true,
                    "Pin items that the player auto-crafts to the top of the terminal");
            this.clearGridOnClose = client.addBoolean("clearGridOnClose", false,
                    "Automatically clear the crafting/encoding grid when closing the terminal");
            this.terminalMargin = client.addInt("terminalMargin", 25,
                    "The vertical margin to apply when sizing terminals. Used to make room for centered item mod search bars");

            // Search Settings
            var search = root.subsection("search");
            this.searchModNameInTooltips = search.addBoolean("searchModNameInTooltips", false,
                    "Should the mod name be included when searching in tooltips.");
            this.useExternalSearch = search.addBoolean("useExternalSearch", false,
                    "Replaces AEs own search with the search of REI or JEI");
            this.clearExternalSearchOnOpen = search.addBoolean("clearExternalSearchOnOpen", true,
                    "When using useExternalSearch, clears the search when the terminal opens");
            this.syncWithExternalSearch = search.addBoolean("syncWithExternalSearch", true,
                    "When REI/JEI is installed, automatically set the AE or REI/JEI search text when either is changed while the terminal is open");
            this.rememberLastSearch = search.addBoolean("rememberLastSearch", true,
                    "Remembers the last search term and restores it when the terminal opens");
            this.autoFocusSearch = search.addBoolean("autoFocusSearch", false,
                    "Automatically focuses the search field when the terminal opens");

            var tooltips = root.subsection("tooltips");
            this.tooltipShowCellUpgrades = tooltips.addBoolean("showCellUpgrades", true,
                    "Show installed upgrades in the tooltips of storage cells, color applicators and matter cannons");
            this.tooltipShowCellContent = tooltips.addBoolean("showCellContent", true,
                    "Show a preview of the content in the tooltips of storage cells, color applicators and matter cannons");
            this.tooltipMaxCellContentShown = tooltips.addInt("maxCellContentShown", 5, 1, 32,
                    "The maximum number of content entries to show in the tooltip of storage cells, color applicators and matter cannons");
        }

    }

    private static class CommonConfig {

        // Misc
        public final IntegerOption formationPlaneEntityLimit;
        public final IntegerOption craftingCalculationTimePerTick;
        public final BooleanOption debugTools;
        public final BooleanOption matterCannonBlockDamage;
        public final BooleanOption tinyTntBlockDamage;
        public final EnumOption<ChannelMode> channels;
        public final BooleanOption spatialAnchorEnableRandomTicks;

        public final IntegerOption growthAcceleratorSpeed;
        public final BooleanOption annihilationPlaneSkyDustGeneration;

        // Spatial IO/Dimension
        public final DoubleOption spatialPowerExponent;
        public final DoubleOption spatialPowerMultiplier;

        // Logging
        public final BooleanOption blockUpdateLog;
        public final BooleanOption craftingLog;
        public final BooleanOption debugLog;
        public final BooleanOption gridLog;
        public final BooleanOption chunkLoggerTrace;

        // Batteries
        public final DoubleOption chargerChargeRate;
        public final IntegerOption wirelessTerminalBattery;
        public final IntegerOption entropyManipulatorBattery;
        public final IntegerOption matterCannonBattery;
        public final IntegerOption portableCellBattery;
        public final IntegerOption colorApplicatorBattery;
        public final IntegerOption chargedStaffBattery;

        // Meteors
        public final BooleanOption spawnPressesInMeteorites;
        public final BooleanOption spawnFlawlessOnly;

        // Wireless
        public final DoubleOption wirelessBaseCost;
        public final DoubleOption wirelessCostMultiplier;
        public final DoubleOption wirelessTerminalDrainMultiplier;
        public final DoubleOption wirelessBaseRange;
        public final DoubleOption wirelessBoosterRangeMultiplier;
        public final DoubleOption wirelessBoosterExp;
        public final DoubleOption wirelessHighWirelessCount;

        // Power Ratios
        public final DoubleOption powerRatioForgeEnergy;
        public final DoubleOption powerUsageMultiplier;
        public final DoubleOption gridEnergyStoragePerNode;
        public final DoubleOption crystalResonanceGeneratorRate;
        public final DoubleOption p2pTunnelEnergyTax;
        public final DoubleOption p2pTunnelTransportTax;

        // Vibration Chamber
        public final DoubleOption vibrationChamberBaseEnergyPerFuelTick;
        public final IntegerOption vibrationChamberMinEnergyPerTick;
        public final IntegerOption vibrationChamberMaxEnergyPerTick;

        // Condenser Power Requirement
        public final IntegerOption condenserMatterBallsPower;
        public final IntegerOption condenserSingularityPower;

        public final Map<TickRates, IntegerOption> tickRateMin = new HashMap<>();
        public final Map<TickRates, IntegerOption> tickRateMax = new HashMap<>();
        public CommonConfig(ConfigSection root) {

            ConfigSection general = root.subsection("general");
            debugTools = general.addBoolean("unsupportedDeveloperTools", Platform.isDevelopmentEnvironment());
            matterCannonBlockDamage = general.addBoolean("matterCannonBlockDamage", true,
                    "Enables the ability of the Matter Cannon to break blocks.");
            tinyTntBlockDamage = general.addBoolean("tinyTntBlockDamage", true,
                    "Enables the ability of Tiny TNT to break blocks.");
            channels = general.addEnum("channels", ChannelMode.DEFAULT,
                    "Changes the channel capacity that cables provide in AE2.");
            spatialAnchorEnableRandomTicks = general.addBoolean("spatialAnchorEnableRandomTicks", true,
                    "Whether Spatial Anchors should force random chunk ticks and entity spawning.");

            ConfigSection automation = root.subsection("automation");
            formationPlaneEntityLimit = automation.addInt("formationPlaneEntityLimit", 128);

            ConfigSection craftingCPU = root.subsection("craftingCPU");
            this.craftingCalculationTimePerTick = craftingCPU.addInt("craftingCalculationTimePerTick", 5);

            var crafting = root.subsection("crafting");
            growthAcceleratorSpeed = crafting.addInt("growthAccelerator", 10, 1, 100,
                    "Number of ticks between two crystal growth accelerator ticks");
            annihilationPlaneSkyDustGeneration = crafting.addBoolean("annihilationPlaneSkyDustGeneration", true,
                "If enabled, an annihilation placed face up at the maximum world height will generate sky stone passively.");

            ConfigSection spatialio = root.subsection("spatialio");
            this.spatialPowerMultiplier = spatialio.addDouble("spatialPowerMultiplier", 1250.0);
            this.spatialPowerExponent = spatialio.addDouble("spatialPowerExponent", 1.35);

            var logging = root.subsection("logging");
            blockUpdateLog = logging.addBoolean("blockUpdateLog", false);
            craftingLog = logging.addBoolean("craftingLog", false);
            debugLog = logging.addBoolean("debugLog", false);
            gridLog = logging.addBoolean("gridLog", false);
            chunkLoggerTrace = logging.addBoolean("chunkLoggerTrace", false,
                    "Enable stack trace logging for the chunk loading debug command");

            ConfigSection battery = root.subsection("battery");
            this.chargerChargeRate = battery.addDouble("chargerChargeRate", 1,
                    0.1, 10,
                    "The chargers charging rate factor, which is applied to the charged items charge rate. 2 means it charges everything twice as fast. 0.5 half as fast.");
            this.wirelessTerminalBattery = battery.addInt("wirelessTerminal", 1600000);
            this.chargedStaffBattery = battery.addInt("chargedStaff", 8000);
            this.entropyManipulatorBattery = battery.addInt("entropyManipulator", 200000);
            this.portableCellBattery = battery.addInt("portableCell", 20000);
            this.colorApplicatorBattery = battery.addInt("colorApplicator", 20000);
            this.matterCannonBattery = battery.addInt("matterCannon", 200000);

            ConfigSection worldGen = root.subsection("worldGen");

            this.spawnPressesInMeteorites = worldGen.addBoolean("spawnPressesInMeteorites", true);
            this.spawnFlawlessOnly = worldGen.addBoolean("spawnFlawlessOnly", false);

            ConfigSection wireless = root.subsection("wireless");
            this.wirelessBaseCost = wireless.addDouble("wirelessBaseCost", 8.0);
            this.wirelessCostMultiplier = wireless.addDouble("wirelessCostMultiplier", 1.0);
            this.wirelessBaseRange = wireless.addDouble("wirelessBaseRange", 16.0);
            this.wirelessBoosterRangeMultiplier = wireless.addDouble("wirelessBoosterRangeMultiplier", 1.0);
            this.wirelessBoosterExp = wireless.addDouble("wirelessBoosterExp", 1.5);
            this.wirelessHighWirelessCount = wireless.addDouble("wirelessHighWirelessCount", 64.0);
            this.wirelessTerminalDrainMultiplier = wireless.addDouble("wirelessTerminalDrainMultiplier", 1.0);

            ConfigSection PowerRatios = root.subsection("PowerRatios");
            powerRatioForgeEnergy = PowerRatios.addDouble("ForgeEnergy", DEFAULT_FE_EXCHANGE);
            powerUsageMultiplier = PowerRatios.addDouble("UsageMultiplier", 1.0, 0.01, Double.MAX_VALUE);
            gridEnergyStoragePerNode = PowerRatios.addDouble("GridEnergyStoragePerNode", 25, 1, 1000000,
                    "How much energy can the internal grid buffer storage per node attached to the grid.");
            crystalResonanceGeneratorRate = PowerRatios.addDouble("CrystalResonanceGeneratorRate", 20, 0, 1000000,
                    "How much energy a crystal resonance generator generates per tick.");
            p2pTunnelEnergyTax = PowerRatios.addDouble("p2pTunnelEnergyTax", 0.025, 0.0, 1.0,
                "The cost to transport energy through an energy P2P tunnel expressed as a factor of the transported energy.");
            p2pTunnelTransportTax = PowerRatios.addDouble("p2pTunnelTransportTax", 0.025, 0.0, 1.0,
                "The cost to transport items/fluids/etc. through P2P tunnels, expressed in AE energy per equivalent I/O bus operation for the transported object type (i.e. items=per 1 item, fluids=per 125mb).");

            ConfigSection Condenser = root.subsection("Condenser");
            condenserMatterBallsPower = Condenser.addInt("MatterBalls", 256);
            condenserSingularityPower = Condenser.addInt("Singularity", 256000);

            ConfigSection tickrates = root.subsection("tickRates",
                    " Min / Max Tickrates for dynamic ticking, most of these components also use sleeping, to prevent constant ticking, adjust with care, non standard rates are not supported or tested.");
            for (TickRates tickRate : TickRates.values()) {
                tickRateMin.put(tickRate, tickrates.addInt(tickRate.name() + "Min", tickRate.getDefaultMin()));
                tickRateMax.put(tickRate, tickrates.addInt(tickRate.name() + "Max", tickRate.getDefaultMax()));
            }

            ConfigSection vibrationChamber = root.subsection("vibrationChamber",
                    "Settings for the Vibration Chamber");
            vibrationChamberBaseEnergyPerFuelTick = vibrationChamber.addDouble("baseEnergyPerFuelTick", 5, 0.1, 1000,
                    "AE energy produced per fuel burn tick (reminder: coal = 1600, block of coal = 16000, lava bucket = 20000 burn ticks)");
            vibrationChamberMinEnergyPerTick = vibrationChamber.addInt("minEnergyPerGameTick", 4, 0, 1000,
                    "Minimum amount of AE/t the vibration chamber can slow down to when energy is being wasted.");
            vibrationChamberMaxEnergyPerTick = vibrationChamber.addInt("baseMaxEnergyPerGameTick", 40, 1, 1000,
                    "Maximum amount of AE/t the vibration chamber can speed up to when generated energy is being fully consumed.");
        }

    }

}
