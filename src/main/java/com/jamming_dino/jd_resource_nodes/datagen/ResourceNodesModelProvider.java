package com.jamming_dino.jd_resource_nodes.datagen;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;

public class ResourceNodesModelProvider extends BlockStateProvider {

    // List of names that should pull from Minecraft namespace for Stone/Deepslate variants
    private static final List<String> VANILLA_NAMES = List.of(
            "copper", "iron", "gold"
    );

    public ResourceNodesModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ResourceNodes.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (DeferredBlock<ResourceNodeBlock> holder : ResourceNodes.REGISTERED_NODES) {
            ResourceNodeBlock block = holder.get();
            String path = holder.getId().getPath(); // e.g. "node_iron_pure"

            // Parse name: "node_iron_pure" -> resourceName="iron", tier=PURE
            String[] parts = path.split("_");
            String tierName = parts[parts.length - 1];
            String resourceName = path.substring(5, path.lastIndexOf("_"));
            ResourceNodeTier tier = getTier(tierName);

            // 1. Determine "Ore" Texture
            ResourceLocation oreTexture;

            if (VANILLA_NAMES.contains(resourceName)) {
                oreTexture = getVanillaTexture(resourceName, tier);
            } else {
                oreTexture = getCustomTexture(resourceName, tier);
            }

            ModelFile oreModel = models().cubeAll(path + "_ore", oreTexture);

            // 2. Determine "Regenerating" Texture (Stone/Tuff/Deepslate)
            ResourceLocation stoneTexture;
            if (block.getBaseBlock() == Blocks.STONE) {
                stoneTexture = ResourceLocation.withDefaultNamespace("block/stone");
            } else if (block.getBaseBlock() == Blocks.TUFF) {
                stoneTexture = ResourceLocation.withDefaultNamespace("block/tuff");
            } else {
                stoneTexture = ResourceLocation.withDefaultNamespace("block/deepslate");
            }

            ModelFile stoneModel = models().cubeAll(path + "_regenerating", stoneTexture);

            // 3. Register BlockState
            getVariantBuilder(block).forAllStates(state -> {
                boolean regenerating = state.getValue(ResourceNodeBlock.REGENERATING);
                return ConfiguredModel.builder()
                        .modelFile(regenerating ? stoneModel : oreModel)
                        .build();
            });

            // 4. Item Model (Block Item)
            simpleBlockItem(block, oreModel);
        }

        // Register Item Models for the Raw Resources
        registerItemModel("bauxite");
        registerItemModel("limestone");
        registerItemModel("sulfur");
        registerItemModel("uranium");
        registerItemModel("caterium");
        registerItemModel("sam");

        // Special case for Raw Quartz (points to vanilla quartz texture)
        itemModels().withExistingParent("raw_quartz", "item/generated")
                .texture("layer0", modLoc("item/quartz"));
    }

    private ResourceLocation getVanillaTexture(String resource, ResourceNodeTier tier) {
        switch (tier) {
            case IMPURE: return ResourceLocation.withDefaultNamespace("block/" + resource + "_ore");
            case NORMAL: return modLoc("block/tuff_" + resource + "_ore");
            case PURE:   return ResourceLocation.withDefaultNamespace("block/deepslate_" + resource + "_ore");
            default:     return ResourceLocation.withDefaultNamespace("block/" + resource + "_ore");
        }
    }

    private ResourceLocation getCustomTexture(String resource, ResourceNodeTier tier) {
        // Custom textures now use consistent naming including stone_ prefix
        switch (tier) {
            case IMPURE: return modLoc("block/stone_" + resource + "_ore");
            case NORMAL: return modLoc("block/tuff_" + resource + "_ore");
            case PURE:   return modLoc("block/deepslate_" + resource + "_ore");
            default:     return modLoc("block/stone_" + resource + "_ore");
        }
    }

    private void registerItemModel(String name) {
        // FIXED: Using withExistingParent explicitly prevents the "item/item/" path doubling
        // 1. "name" -> creates assets/jd_resource_nodes/models/item/{name}.json
        // 2. "item/generated" -> inherits from vanilla item model
        // 3. modLoc("item/" + name) -> uses texture assets/jd_resource_nodes/textures/item/{name}.png
        itemModels().withExistingParent(name, "item/generated")
                .texture("layer0", modLoc("item/" + name));
    }

    private ResourceNodeTier getTier(String name) {
        if (name.equals("impure")) return ResourceNodeTier.IMPURE;
        if (name.equals("pure")) return ResourceNodeTier.PURE;
        return ResourceNodeTier.NORMAL;
    }
}