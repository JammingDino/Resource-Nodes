package com.jamming_dino.jd_resource_nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
        } catch (IOException e) {
            ResourceNodes.LOGGER.error("Failed to save config.", e);
        }
    }
}