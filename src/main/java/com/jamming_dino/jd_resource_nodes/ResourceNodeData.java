package com.jamming_dino.jd_resource_nodes;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * Stores metadata about resource nodes including their output items
 */
public class ResourceNodeData {

    private final String category; // e.g. "iron", "gold", "diamond"
    private final Item outputItem; // e.g. Items.RAW_IRON
    private final ItemStack displayStack; // What to show in the radial menu
    private final List<Block> nodes; // All tiers of this node type

    public ResourceNodeData(String category, Item outputItem, ItemStack displayStack) {
        this.category = category;
        this.outputItem = outputItem;
        this.displayStack = displayStack;
        this.nodes = new ArrayList<>();
    }

    public String getCategory() {
        return category;
    }

    public Item getOutputItem() {
        return outputItem;
    }

    public ItemStack getDisplayStack() {
        return displayStack;
    }

    public List<Block> getNodes() {
        return nodes;
    }

    public void addNode(Block node) {
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }

    // Registry to store all node data
    private static final Map<String, ResourceNodeData> REGISTRY = new LinkedHashMap<>();

    /**
     * Register a new resource node category
     */
    public static ResourceNodeData register(String category, Item outputItem, ItemStack displayStack) {
        ResourceNodeData data = new ResourceNodeData(category, outputItem, displayStack);
        REGISTRY.put(category, data);
        return data;
    }

    /**
     * Get all registered node categories
     */
    public static List<ResourceNodeData> getAllCategories() {
        return new ArrayList<>(REGISTRY.values());
    }

    /**
     * Get node data by category name
     */
    public static ResourceNodeData getByCategory(String category) {
        return REGISTRY.get(category);
    }

    /**
     * Find which category a specific block belongs to
     */
    public static ResourceNodeData findCategoryForBlock(Block block) {
        for (ResourceNodeData data : REGISTRY.values()) {
            if (data.nodes.contains(block)) {
                return data;
            }
        }
        return null;
    }
}