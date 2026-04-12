package com.jamming_dino.jd_resource_nodes;

import net.minecraft.resources.ResourceLocation;

/**
 * Parsed custom node configuration entry with unresolved resource IDs.
 */
public record CustomResourceNodeDefinition(
        String id,
        String category,
        ResourceLocation readyBlockId,
        ResourceLocation regeneratingBlockId,
        ResourceLocation outputItemId,
        String overlaySource,
        CustomNodePurityMode purityMode
) {
    public static CustomResourceNodeDefinition fromConfig(ResourceNodesConfig.CustomNodeConfig config) {
        if (config == null || config.id == null || config.id.isBlank()) {
            return null;
        }

        ResourceLocation readyBlockId = parseId(config.ready_block);
        ResourceLocation regeneratingBlockId = parseId(config.regenerating_block);
        ResourceLocation outputItemId = parseId(config.output_item);

        if (readyBlockId == null || regeneratingBlockId == null || outputItemId == null) {
            return null;
        }

        String sanitizedId = sanitizeKey(config.id);
        String category = config.category == null || config.category.isBlank() ? sanitizedId : sanitizeKey(config.category);
        String overlaySource = config.overlay_source == null || config.overlay_source.isBlank()
                ? sanitizedId
                : sanitizeKey(config.overlay_source);
        CustomNodePurityMode purityMode = CustomNodePurityMode.fromId(config.purity_mode);

        return new CustomResourceNodeDefinition(
                sanitizedId,
                category,
                readyBlockId,
                regeneratingBlockId,
                outputItemId,
                overlaySource,
                purityMode
        );
    }

    private static ResourceLocation parseId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(id.trim());
    }

    private static String sanitizeKey(String key) {
        return key.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }
}





