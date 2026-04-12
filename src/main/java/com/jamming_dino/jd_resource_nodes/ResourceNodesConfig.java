package com.jamming_dino.jd_resource_nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceNodesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("jd_resource_nodes.json").toFile();

    private static ResourceNodesConfig INSTANCE;

    // Regeneration tick settings - Single value for all tiers now
    public int regeneration_ticks = 600;

    // Pinger display settings
    public boolean text_enabled = true;
    public float text_scale = 1.0f;

    // Scanner settings
    public int scanner_radius = 128;

    // Optional map-pack custom nodes.
    public List<CustomNodeConfig> custom_nodes = new ArrayList<>();

    // Global (pack/server) node disable list.
    public List<String> disabled_nodes_global = new ArrayList<>();

    public static int getRegenerateTicks() {
        return INSTANCE.regeneration_ticks;
    }

    public static boolean isTextEnabled() {
        return INSTANCE.text_enabled;
    }

    public static float getTextScale() {
        return INSTANCE.text_scale;
    }

    public static int getScannerRadius() {
        return INSTANCE.scanner_radius;
    }

    public static List<CustomNodeConfig> getCustomNodes() {
        return INSTANCE.custom_nodes;
    }

    public static List<String> getDisabledNodesGlobal() {
        return INSTANCE.disabled_nodes_global;
    }

    public static boolean isNodeGloballyEnabled(String blockId) {
        return !INSTANCE.disabled_nodes_global.contains(blockId);
    }

    public static void setNodeGloballyEnabled(String blockId, boolean enabled) {
        if (blockId == null || blockId.isBlank()) {
            return;
        }

        if (enabled) {
            INSTANCE.disabled_nodes_global.remove(blockId);
        } else if (!INSTANCE.disabled_nodes_global.contains(blockId)) {
            INSTANCE.disabled_nodes_global.add(blockId);
        }

        ResourceNodes.LOGGER.info("Set global node state: {} -> {}", blockId, enabled ? "ENABLED" : "DISABLED");
        save();
    }

    public static void addCustomNode(CustomNodeConfig config) {
        if (config == null) {
            return;
        }

        String incomingId = sanitizeId(config.id);
        for (int i = 0; i < INSTANCE.custom_nodes.size(); i++) {
            CustomNodeConfig existing = INSTANCE.custom_nodes.get(i);
            if (sanitizeId(existing.id).equals(incomingId)) {
                INSTANCE.custom_nodes.set(i, config);
                ResourceNodes.LOGGER.info("Updated custom node config id='{}'", incomingId);
                save();
                return;
            }
        }

        INSTANCE.custom_nodes.add(config);
        ResourceNodes.LOGGER.info("Added custom node config id='{}'", incomingId);
        save();
    }

    private static String sanitizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }

    public static void setRegenerateTicks(int ticks) {
        INSTANCE.regeneration_ticks = Math.max(1, Math.min(72000, ticks)); // Clamp 1-72000 (1 tick to 1 hour)
        save();
    }

    public static void setTextEnabled(boolean enabled) {
        INSTANCE.text_enabled = enabled;
        save();
    }

    public static void setTextScale(float scale) {
        INSTANCE.text_scale = Math.max(0.1f, Math.min(5.0f, scale)); // Clamp between 0.1 and 5.0
        save();
    }

    public static void setScannerRadius(int radius) {
        INSTANCE.scanner_radius = Math.max(16, Math.min(512, radius)); // Clamp between 16 (1 chunk) and 512 (32 chunks)
        save();
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ResourceNodesConfig loaded = GSON.fromJson(reader, ResourceNodesConfig.class);

                if (loaded == null) {
                    INSTANCE = new ResourceNodesConfig();
                } else {
                    INSTANCE = loaded;

                    // Validate loaded values
                    if (INSTANCE.text_scale < 0.1f || INSTANCE.text_scale > 5.0f) {
                        INSTANCE.text_scale = 1.0f;
                    }
                    if (INSTANCE.regeneration_ticks < 1 || INSTANCE.regeneration_ticks > 72000) {
                        INSTANCE.regeneration_ticks = 600;
                    }
                    if (INSTANCE.scanner_radius < 16 || INSTANCE.scanner_radius > 512) {
                        INSTANCE.scanner_radius = 128;
                    }
                    if (INSTANCE.custom_nodes == null) {
                        INSTANCE.custom_nodes = new ArrayList<>();
                    } else {
                        Map<String, CustomNodeConfig> deduped = new LinkedHashMap<>();
                        for (CustomNodeConfig custom : INSTANCE.custom_nodes) {
                            deduped.put(sanitizeId(custom.id), custom);
                        }
                        INSTANCE.custom_nodes = new ArrayList<>(deduped.values());
                    }
                    if (INSTANCE.disabled_nodes_global == null) {
                        INSTANCE.disabled_nodes_global = new ArrayList<>();
                    }
                }
                save();
            } catch (IOException e) {
                ResourceNodes.LOGGER.error("Failed to load config, using defaults.", e);
                INSTANCE = new ResourceNodesConfig();
                save();
            }
        } else {
            INSTANCE = new ResourceNodesConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
            ResourceNodes.LOGGER.info("Saved config file: {}", CONFIG_FILE.getAbsolutePath());
        } catch (IOException e) {
            ResourceNodes.LOGGER.error("Failed to save config.", e);
        }
    }

    public static class CustomNodeConfig {
        public String id = "";
        public String category = "";
        public String purity_mode = "all";


        // Block visual when node is ready to mine.
        public String ready_block = "minecraft:stone";

        // Block visual while regenerating.
        public String regenerating_block = "minecraft:stone";

        // Main output used by scanner category display.
        public String output_item = "minecraft:raw_iron";

        // Uses existing node overlay textures, e.g. iron, diamond, nether_quartz.
        public String overlay_source = "iron";
    }
}