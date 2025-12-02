# JD Resource Nodes

![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange) ![MC Version](https://img.shields.io/badge/Minecraft-1.21.1-green) ![License](https://img.shields.io/badge/License-CC0-blue)

**JD Resource Nodes** adds renewable versions of every vanilla ore to Minecraft. Instead of disappearing when mined, these nodes deplete into their base block (Stone, Deepslate, or Netherrack) and regenerate over time, making them perfect for static mining setups and automation.

## Features

*   **Complete Vanilla Coverage:** Includes Coal, Iron, Copper, Gold, Redstone, Lapis, Emerald, Diamond, Quartz, and Ancient Debris.
*   **Deepslate & Nether Variants:** Automatically adapts textures and base blocks (e.g., Deepslate Ores turn into Deepslate).
*   **Three Tiers:** Every node comes in **Impure**, **Normal**, and **Pure** variants, determining how fast they regenerate.
*   **Visual Indicators:** Nodes feature custom overlaid borders to easily distinguish their tier.
*   **Automation Ready:** Fully compatible with quarries and mods like **Create**.

## How It Works

### Permanent Infrastructure
In Survival Mode, Resource Nodes are designed to be **permanent infrastructure**.
*   **Immovable:** Nodes cannot be moved by Pistons or picked up (even with Silk Touch).
*   **Indestructible:** Nodes cannot be destroyed by players in Survival mode or by explosions.
*   **Removal:** The only way to remove a node is to break it in **Creative Mode**.

### Mining & Regeneration
When you mine a Resource Node, it drops its standard loot (e.g., Raw Iron) and turns into its base block (e.g., Stone).
*   **Unbreakable State:** While regenerating (Stone state), the block has the hardness of Bedrock (`-1.0f`).
*   **Automation:** Create Drills and other machines will automatically stop mining when the node hits this state and resume immediately once it turns back into Ore.

### Admin Overrides
If you need to mass-remove nodes using WorldEdit or Commands (since they resist being set to Air), they have a weakness to **Barriers**.
*   **Command:** `/fill x1 y1 z1 x2 y2 z2 minecraft:barrier` will successfully delete the nodes.
*   You can then replace the barriers with air.

## Configuration

The regeneration speeds can be adjusted in `config/jd_resource_nodes.json`.
*Defaults:*
*   **Impure:** 1200 ticks (60 seconds)
*   **Normal:** 600 ticks (30 seconds)
*   **Pure:** 200 ticks (10 seconds)

## Installation

1.  Download and run the **NeoForge Installer** for Minecraft 1.21.1.
2.  Drop `jd_resource_nodes.jar` into your `mods` folder.

---

*Created by jammingdino*