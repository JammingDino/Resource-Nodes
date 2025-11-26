package jammingdino.jd_resource_nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ResourceNodesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("jd_resource_nodes.json").toFile();

    // The single instance that holds our values
    private static ResourceNodesConfig INSTANCE;

    // --- Config Values (Defaults) ---
    public int impure_ticks = 1200; // 60 seconds
    public int normal_ticks = 600;  // 30 seconds
    public int pure_ticks = 200;    // 10 seconds

    // --- Getters ---
    public static int getImpureTicks() {
        return INSTANCE.impure_ticks;
    }

    public static int getNormalTicks() {
        return INSTANCE.normal_ticks;
    }

    public static int getPureTicks() {
        return INSTANCE.pure_ticks;
    }

    // --- Loading Logic ---
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
            save(); // Create the file so the user can see it
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