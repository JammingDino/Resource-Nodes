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
    // In NeoForge, we use FMLPaths to get the config directory
    private static final File CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("jd_resource_nodes.json").toFile();

    private static ResourceNodesConfig INSTANCE;

    public int impure_ticks = 1200;
    public int normal_ticks = 600;
    public int pure_ticks = 200;

    public static int getImpureTicks() { return INSTANCE.impure_ticks; }
    public static int getNormalTicks() { return INSTANCE.normal_ticks; }
    public static int getPureTicks() { return INSTANCE.pure_ticks; }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ResourceNodesConfig.class);
            } catch (IOException e) {
                ResourceNodes.LOGGER.error("Failed to load config, using defaults.", e);
                INSTANCE = new ResourceNodesConfig();
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