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

    // Regeneration tick settings - MUST be int, not float!
    public int impure_ticks = 1200;
    public int normal_ticks = 600;
    public int pure_ticks = 200;

    // Pinger display settings
    public boolean text_enabled = true;
    public float text_scale = 1.0f;

    public static int getImpureTicks() {
        return INSTANCE.impure_ticks;
    }

    public static int getNormalTicks() {
        return INSTANCE.normal_ticks;
    }

    public static int getPureTicks() {
        return INSTANCE.pure_ticks;
    }

    public static boolean isTextEnabled() {
        return INSTANCE.text_enabled;
    }

    public static float getTextScale() {
        return INSTANCE.text_scale;
    }

    public static void setImpureTicks(int ticks) {
        INSTANCE.impure_ticks = Math.max(1, Math.min(72000, ticks)); // Clamp 1-72000 (1 tick to 1 hour)
        save();
    }

    public static void setNormalTicks(int ticks) {
        INSTANCE.normal_ticks = Math.max(1, Math.min(72000, ticks)); // Clamp 1-72000 (1 tick to 1 hour)
        save();
    }

    public static void setPureTicks(int ticks) {
        INSTANCE.pure_ticks = Math.max(1, Math.min(72000, ticks)); // Clamp 1-72000 (1 tick to 1 hour)
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

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ResourceNodesConfig loaded = GSON.fromJson(reader, ResourceNodesConfig.class);

                // If loaded config is null or missing values, use defaults
                if (loaded == null) {
                    INSTANCE = new ResourceNodesConfig();
                } else {
                    INSTANCE = loaded;

                    // Validate loaded values
                    if (INSTANCE.text_scale < 0.1f || INSTANCE.text_scale > 5.0f) {
                        INSTANCE.text_scale = 1.0f;
                    }
                    if (INSTANCE.impure_ticks < 1 || INSTANCE.impure_ticks > 72000) {
                        INSTANCE.impure_ticks = 1200;
                    }
                    if (INSTANCE.normal_ticks < 1 || INSTANCE.normal_ticks > 72000) {
                        INSTANCE.normal_ticks = 600;
                    }
                    if (INSTANCE.pure_ticks < 1 || INSTANCE.pure_ticks > 72000) {
                        INSTANCE.pure_ticks = 200;
                    }
                    // Note: text_enabled defaults to true in the class definition
                    // If the JSON doesn't have this field, Java will use the default value
                }
                save(); // Save to ensure file is up to date with any corrections
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