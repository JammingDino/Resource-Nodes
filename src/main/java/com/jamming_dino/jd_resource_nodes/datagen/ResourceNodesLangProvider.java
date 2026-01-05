package com.jamming_dino.jd_resource_nodes.datagen;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredBlock;

public class ResourceNodesLangProvider extends LanguageProvider {
    public ResourceNodesLangProvider(PackOutput output, String locale) {
        super(output, ResourceNodes.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.jd_resource_nodes.general", "Resource Nodes");
        add("key.jd_resource_nodes.ping", "Resource Node Scan");
        add("key.categories.jd_resource_nodes", "Resource Nodes");

        // Config screen translations
        add("config.jd_resource_nodes.title", "Resource Nodes Config");
        add("config.jd_resource_nodes.text_enabled", "Pinger Text");
        add("config.jd_resource_nodes.text_scale", "Text Scale");
        add("config.jd_resource_nodes.scanner_radius", "Scanner Radius"); // Added
        add("config.jd_resource_nodes.regeneration", "Regeneration Settings");
        add("config.jd_resource_nodes.impure_ticks", "Impure Regeneration Ticks");
        add("config.jd_resource_nodes.normal_ticks", "Normal Regeneration Ticks");
        add("config.jd_resource_nodes.pure_ticks", "Pure Regeneration Ticks");

        for (DeferredBlock<ResourceNodeBlock> holder : ResourceNodes.REGISTERED_NODES) {
            String path = holder.getId().getPath();
            add(holder.get(), generateDisplayName(path));
        }
    }

    private String generateDisplayName(String path) {
        // "node_iron_pure" -> "Node Iron Pure"
        String spaced = path.replace('_', ' ');
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : spaced.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}