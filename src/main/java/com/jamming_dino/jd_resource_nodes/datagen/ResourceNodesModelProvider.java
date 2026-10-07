package com.jamming_dino.jd_resource_nodes.datagen;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ResourceNodesModelProvider extends BlockStateProvider {
    private final ExistingFileHelper fileHelper;

    public ResourceNodesModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, ResourceNodes.MODID, exFileHelper);
        this.fileHelper = exFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        for (RegistryObject<ResourceNodeBlock> holder : ResourceNodes.REGISTERED_NODES) {
            ResourceNodeBlock block = holder.get();
            String path = holder.getId().getPath(); // e.g. "node_iron_pure"

            ResourceNodeTier tier = getTierFromPath(path);
            String tierName = tier.getSerializedName();

            // 1. Determine Texture Names
            boolean isDebris = block.getOriginalOre() == Blocks.ANCIENT_DEBRIS;

            ModelFile oreModel;
            ModelFile stoneModel;

            String baseBlockName = BuiltInRegistries.BLOCK.getKey(block.getBaseBlock()).getPath();
            ResourceLocation stoneTexture = existingTextureOrFallback(
                    modLoc("block/node_" + baseBlockName + "_" + tierName),
                    new ResourceLocation("minecraft", "block/stone")
            );

            if (isDebris) {
                ResourceLocation sideTex = existingTextureOrFallback(
                        modLoc("block/node_ancient_debris_" + tierName + "_side"),
                        new ResourceLocation("minecraft", "block/ancient_debris_side")
                );
                ResourceLocation topTex = existingTextureOrFallback(
                        modLoc("block/node_ancient_debris_" + tierName + "_top"),
                        new ResourceLocation("minecraft", "block/ancient_debris_top")
                );

                oreModel = models().cubeColumn(path + "_ore", sideTex, topTex);
                stoneModel = models().cubeAll(path + "_stone", stoneTexture);
            } else {
                ResourceLocation oreTexture = existingTextureOrFallback(
                        modLoc("block/" + path),
                        BuiltInRegistries.BLOCK.getKey(block.getOriginalOre()).withPrefix("block/")
                );

                oreModel = models().cubeAll(path + "_ore", oreTexture);
                stoneModel = models().cubeAll(path + "_stone", stoneTexture);
            }

            // 2. Register BlockState
            getVariantBuilder(block).forAllStates(state -> {
                boolean regenerating = state.getValue(ResourceNodeBlock.REGENERATING);
                return ConfiguredModel.builder()
                        .modelFile(regenerating ? stoneModel : oreModel)
                        .build();
            });

            // 3. Item Model
            simpleBlockItem(block, oreModel);
        }
    }

    private ResourceLocation existingTextureOrFallback(ResourceLocation preferred, ResourceLocation fallback) {
        if (fileHelper.exists(preferred, PackType.CLIENT_RESOURCES, ".png", "textures")) {
            return preferred;
        }
        return fallback;
    }

    private ResourceNodeTier getTierFromPath(String path) {
        if (path.endsWith("impure")) return ResourceNodeTier.IMPURE;
        if (path.endsWith("pure")) return ResourceNodeTier.PURE;
        return ResourceNodeTier.NORMAL;
    }
}