package com.jamming_dino.jd_resource_nodes;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TextureBaker {

    private static final Map<String, String[]> NODES = new HashMap<>();

    static {
        NODES.put("coal", new String[]{"coal_ore", "stone"});
        NODES.put("deepslate_coal", new String[]{"deepslate_coal_ore", "deepslate"});
        NODES.put("iron", new String[]{"iron_ore", "stone"});
        NODES.put("deepslate_iron", new String[]{"deepslate_iron_ore", "deepslate"});
        NODES.put("copper", new String[]{"copper_ore", "stone"});
        NODES.put("deepslate_copper", new String[]{"deepslate_copper_ore", "deepslate"});
        NODES.put("gold", new String[]{"gold_ore", "stone"});
        NODES.put("deepslate_gold", new String[]{"deepslate_gold_ore", "deepslate"});
        NODES.put("nether_gold", new String[]{"nether_gold_ore", "netherrack"});
        NODES.put("redstone", new String[]{"redstone_ore", "stone"});
        NODES.put("deepslate_redstone", new String[]{"deepslate_redstone_ore", "deepslate"});
        NODES.put("lapis", new String[]{"lapis_ore", "stone"});
        NODES.put("deepslate_lapis", new String[]{"deepslate_lapis_ore", "deepslate"});
        NODES.put("emerald", new String[]{"emerald_ore", "stone"});
        NODES.put("deepslate_emerald", new String[]{"deepslate_emerald_ore", "deepslate"});
        NODES.put("diamond", new String[]{"diamond_ore", "stone"});
        NODES.put("deepslate_diamond", new String[]{"deepslate_diamond_ore", "deepslate"});
        NODES.put("nether_quartz", new String[]{"nether_quartz_ore", "netherrack"});
    }

    public static void main(String[] args) throws IOException {
        String projectPath = System.getProperty("user.dir");
        Path inputDir = Paths.get(projectPath, "src", "main", "resources", "assets", "jd_resource_nodes", "textures", "block", "input");
        Path outputDir = Paths.get(projectPath, "src", "main", "resources", "assets", "jd_resource_nodes", "textures", "block");

        // --- DEBUG PRINTING ---
        System.out.println("Working Directory: " + projectPath);
        System.out.println("Looking for inputs at: " + inputDir.toAbsolutePath());

        if (!inputDir.toFile().exists()) {
            System.err.println("ERROR: Input directory does not exist!");
            System.err.println("Expected: " + inputDir.toAbsolutePath());
            return;
        }

        System.out.println("Starting Texture Baking...");

        String[] tiers = {"impure", "normal", "pure"};

        // 1. Check Borders specifically
        for (String tier : tiers) {
            String filename = "border_" + tier + ".png";
            File f = inputDir.resolve(filename).toFile();
            if (!f.exists()) {
                System.err.println("MISSING BORDER: " + f.getAbsolutePath());
            } else {
                System.out.println("Found border: " + filename);
            }
        }

        // 2. Process Standard Nodes
        for (Map.Entry<String, String[]> entry : NODES.entrySet()) {
            String nodeName = entry.getKey();
            String oreTex = entry.getValue()[0];
            String baseTex = entry.getValue()[1];

            for (String tier : tiers) {
                bake(inputDir, outputDir, oreTex, "border_" + tier, "node_" + nodeName + "_" + tier);
                bake(inputDir, outputDir, baseTex, "border_" + tier, "node_" + baseTex + "_" + tier);
            }
        }

        // 3. Process Ancient Debris
        for (String tier : tiers) {
            bake(inputDir, outputDir, "ancient_debris_side", "border_" + tier, "node_ancient_debris_" + tier + "_side");
            bake(inputDir, outputDir, "ancient_debris_top", "border_" + tier, "node_ancient_debris_" + tier + "_top");
            bake(inputDir, outputDir, "netherrack", "border_" + tier, "node_netherrack_" + tier);
        }

        System.out.println("Texture Baking Complete! Refresh your project.");
    }

    private static void bake(Path in, Path out, String baseName, String overlayName, String outputName) {
        try {
            File baseFile = in.resolve(baseName + ".png").toFile();
            File overlayFile = in.resolve(overlayName + ".png").toFile();
            File outFile = out.resolve(outputName + ".png").toFile();

            if (!baseFile.exists()) {
                // Don't spam console if the base file is missing, we likely already know
                return;
            }
            if (!overlayFile.exists()) {
                return;
            }

            BufferedImage base = ImageIO.read(baseFile);
            BufferedImage overlay = ImageIO.read(overlayFile);
            BufferedImage combined = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = combined.createGraphics();
            g.drawImage(base, 0, 0, null);
            g.drawImage(overlay, 0, 0, null);
            g.dispose();

            ImageIO.write(combined, "PNG", outFile);
            System.out.println("Generated: " + outFile.getName());

        } catch (IOException e) {
            System.err.println("Failed to write " + outputName);
            e.printStackTrace();
        }
    }
}