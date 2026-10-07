# JD Resource Nodes

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-orange)](https://neoforged.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://minecraft.wiki/w/Java_Edition_1.21.1)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/kQTp3U9Z?color=00AF5C&logo=modrinth&label=Downloads)](https://modrinth.com/mod/resource-nodes)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1419540?logo=curseforge&label=Downloads)](https://www.curseforge.com/minecraft/mc-mods/resource-nodes)

**JD Resource Nodes** adds renewable versions of every vanilla ore to Minecraft. Instead of disappearing when mined, these nodes deplete into their base block (Stone, Deepslate, or Netherrack) and regenerate over time, making them perfect for static mining setups and automation.

## Features

*   **Complete Vanilla Coverage:** Includes Coal, Iron, Copper, Gold, Redstone, Lapis, Emerald, Diamond, Quartz, and Ancient Debris.
*   **Deepslate & Nether Variants:** Automatically adapts textures and base blocks (e.g., Deepslate Ores turn into Deepslate).
*   **Three Tiers:** Every node comes in **Impure**, **Normal**, and **Pure** variants, determining how fast they regenerate.
*   **Visual Indicators:** Nodes feature custom overlaid borders to easily distinguish their tier.
*   **Unlockable Scanner:** A built-in visual scanner with a command-based progression system.
*   **Automation Ready:** Fully compatible with quarries and mods like **Create**.

## How It Works

Nodes generate naturally: in new chunks, about 1 in every 1000 vanilla ores (Overworld and Nether) is replaced by a node of a random purity. Map and pack makers can also place them by hand.

### Permanent Infrastructure
Resource Nodes are designed to be **permanent infrastructure**.
*   **Immovable:** Nodes cannot be moved by Pistons.
*   **Explosion-proof:** Explosions do not destroy nodes.
*   **Normal mining** collects the drops and leaves the node to regenerate; it is never destroyed.

### Removing a Node in Survival
If a node spawned somewhere you don't want it:
*   **Sneak + mine** it to remove it for good. It drops one of its resource (e.g. 1 Raw Iron), not the node.
*   **Silk Touch** picks the node up as an item so you can place it elsewhere.
*   In **Creative Mode**, breaking a node removes it.

### Mining & Regeneration
When you mine a Resource Node, it drops its standard loot (e.g., Raw Iron) and turns into its base block (e.g., Stone).
*   **Unbreakable State:** While regenerating (Stone state), the block has the hardness of Bedrock (`-1.0f`).
*   **Automation:** Create Drills and other machines will automatically stop mining when the node hits this state and resume immediately once it turns back into Ore.

### Resource Scanner & Progression
The mod includes a visual scanner to help locate nodes through walls.
*   **Usage:** Press and hold **`V`** to open the radial menu, select a resource, and release to scan.
*   **Progression:** By default, the scanner is **locked**. Players must unlock the ability to scan for specific resources via commands. This is designed for modpack makers to gate resources behind quests or milestones.

**Scanner Commands (Requires OP):**
*   `/scanner unlock <resource>` - Unlocks a specific node type (e.g., `/scanner unlock iron`).
*   `/scanner unlock all` - Instantly unlocks every registered resource node.
*   `/scanner lock <resource>` - Removes the ability to scan for a specific resource.
*   `/scanner lock all` - Resets the scanner, locking all nodes.

### Admin Overrides
If you need to mass-remove nodes using WorldEdit or Commands (since they resist being set to Air), they have a weakness to **Barriers**.
*   **Command:** `/fill x1 y1 z1 x2 y2 z2 minecraft:barrier` will successfully delete the nodes.
*   You can then replace the barriers with air.

## Configuration

Settings live in `config/jd_resource_nodes.json` and the in-game config screen.
*   **`regeneration_ticks`** (default `600`, 30 seconds): regeneration time for every node. Purity sets the drop amount instead: Impure 1, Normal 2, Pure 4.
*   **`ore_replacement_enabled`** (default `true`): turn natural node generation on or off.
*   **`ore_replacement_chance`** (default `1000`): 1 in this many ores becomes a node.
*   **`impure_weight` / `normal_weight` / `pure_weight`** (defaults `60` / `30` / `10`): how common each purity is. These are relative weights; `0` disables a purity.

Ore replacement only affects chunks generated after the mod is installed.

## Installation

1.  Download and run the **NeoForge Installer** for Minecraft 1.21.1.
2.  Drop `jd_resource_nodes.jar` into your `mods` folder.

---

*Created by jammingdino*
