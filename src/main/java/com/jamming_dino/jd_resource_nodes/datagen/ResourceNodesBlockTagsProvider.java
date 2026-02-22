package com.jamming_dino.jd_resource_nodes.datagen;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ResourceNodesBlockTagsProvider extends BlockTagsProvider {

    public static final TagKey<Block> CREATE_NON_MOVABLE = BlockTags.create(ResourceLocation.fromNamespaceAndPath("create", "non_movable"));

    public ResourceNodesBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ResourceNodes.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredBlock<ResourceNodeBlock> holder : ResourceNodes.REGISTERED_NODES) {
            ResourceNodeBlock block = holder.get();
            Block original = block.getOriginalOre();

            // 1. Prevent Create Contraptions from moving these blocks
            tag(CREATE_NON_MOVABLE).add(block);

            // 2. All nodes are mineable with a pickaxe
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);

            // 3. Tiers (Mining Level)

            // Vanilla-logic ores
            if (original == Blocks.ANCIENT_DEBRIS) {
                tag(BlockTags.NEEDS_DIAMOND_TOOL).add(block);
            }
            else if (original == Blocks.DIAMOND_ORE || original == Blocks.DEEPSLATE_DIAMOND_ORE ||
                    original == Blocks.EMERALD_ORE || original == Blocks.DEEPSLATE_EMERALD_ORE ||
                    original == Blocks.GOLD_ORE || original == Blocks.DEEPSLATE_GOLD_ORE ||
                    original == Blocks.REDSTONE_ORE || original == Blocks.DEEPSLATE_REDSTONE_ORE) {
                tag(BlockTags.NEEDS_IRON_TOOL).add(block);
            }
            // Catch-all for Custom Ores (original is AIR) + Iron + Copper + Lapis
            // This ensures Bauxite, SAM, etc require a Stone Pickaxe or better
            else {
                tag(BlockTags.NEEDS_STONE_TOOL).add(block);
            }
        }
    }
}