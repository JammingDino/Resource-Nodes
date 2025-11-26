# JD Resource Nodes

![Fabric](https://img.shields.io/badge/Loader-Fabric-beige) ![MC Version](https://img.shields.io/badge/Minecraft-1.20.1-green) ![License](https://img.shields.io/badge/License-APACHE-blue)

**JD Resource Nodes** adds renewable versions of every vanilla ore to Minecraft. Instead of disappearing when mined, these nodes deplete into their base block (Stone, Deepslate, or Netherrack) and regenerate over time, making them perfect for static mining setups and automation.

## Features

*   **Complete Vanilla Coverage:** Includes Coal, Iron, Copper, Gold, Redstone, Lapis, Emerald, Diamond, Quartz, and Ancient Debris.
*   **Deepslate & Nether Variants:** Automatically adapts textures and base blocks (e.g., Deepslate Ores turn into Deepslate).
*   **Three Tiers:** Every node comes in **Impure**, **Normal**, and **Pure** variants, determining how fast they regenerate.
*   **Automation Ready:** Fully compatible with quarries, TNT, and mods like **Create**. Drills will harvest resources without destroying the node.
*   **Configurable:** Purity timings are fully customizable via a JSON config file.

## How It Works

### Mining & Regeneration
When you mine a Resource Node, it drops its standard loot (e.g., Raw Iron) and turns into its base block (e.g., Stone). It becomes **unbreakable** in this state. After a specific cooldown, it turns back into the ore, ready to be mined again.

### Interaction Logic
*   **Standard Mining:** Harvests the resource and triggers regeneration.
*   **Crouch (Sneak) + Mine:** Permanently destroys the node (drops the ore items).
*   **Silk Touch:** Mines the Node block itself, allowing you to pick it up and move it.
*   **Creative Mode:** Instantly destroys the node.

### Automation
Resource Nodes are designed to be "Immortal" against machines.
*   **Create Drills / Quarries:** Will harvest the items and trigger the cooldown. If a machine tries to mine the "Stone" state, the block resists destruction and preserves its regeneration timer.
*   **Explosions:** Nodes are resistant to TNT destruction and will regenerate instead of disappearing.

## Configuration

The regeneration speeds can be adjusted in `config/jd_resource_nodes.json`.
*Defaults:*
*   **Impure:** 1200 ticks (60 seconds)
*   **Normal:** 600 ticks (30 seconds)
*   **Pure:** 200 ticks (10 seconds)

## Installation

1.  Download and install **Fabric Loader** for Minecraft 1.20.1.
2.  Download the **Fabric API** mod.
3.  Drop `fabric-api.jar` and `jd_resource_nodes.jar` into your `mods` folder.

---

*Created by jammingdino*
