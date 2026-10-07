package com.jamming_dino.jd_resource_nodes.world;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs once per chunk after ores are placed and swaps a configurable fraction of
 * vanilla ores for the matching built-in resource node.
 */
public class OreToNodeFeature extends Feature<NoneFeatureConfiguration> {
    private static volatile Map<Block, Map<ResourceNodeTier, Block>> oreToNodes;

    public OreToNodeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!ResourceNodesConfig.isOreReplacementEnabled()) return false;

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ChunkAccess chunk = level.getChunk(context.origin());
        Map<Block, Map<ResourceNodeTier, Block>> nodes = getOreToNodes();
        int chance = ResourceNodesConfig.getOreReplacementChance();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        LevelChunkSection[] sections = chunk.getSections();
        for (int s = 0; s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section.hasOnlyAir()) continue;
            int baseY = chunk.getSectionYFromSectionIndex(s) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Map<ResourceNodeTier, Block> tiers = nodes.get(section.getBlockState(x, y, z).getBlock());
                        if (tiers == null || random.nextInt(chance) != 0) continue;
                        Block node = tiers.get(ResourceNodesConfig.rollTier(random));
                        if (node == null || !ResourceNodesConfig.isNodeGloballyEnabled(BuiltInRegistries.BLOCK.getKey(node).toString())) continue;
                        level.setBlock(pos.set(minX + x, baseY + y, minZ + z), node.defaultBlockState(), Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    // Built-in nodes only: custom nodes can use plain blocks (e.g. stone) as their "ore".
    private static Map<Block, Map<ResourceNodeTier, Block>> getOreToNodes() {
        if (oreToNodes == null) {
            Map<Block, Map<ResourceNodeTier, Block>> map = new HashMap<>();
            for (RegistryObject<ResourceNodeBlock> entry : ResourceNodes.REGISTERED_NODES) {
                ResourceNodeBlock node = entry.get();
                if (entry.getId().getPath().startsWith("node_custom_")) continue;
                map.computeIfAbsent(node.getOriginalOre(), k -> new EnumMap<>(ResourceNodeTier.class)).put(node.getTier(), node);
            }
            oreToNodes = map;
        }
        return oreToNodes;
    }
}
