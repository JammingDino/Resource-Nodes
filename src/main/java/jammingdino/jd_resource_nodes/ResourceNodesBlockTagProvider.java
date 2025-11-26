package jammingdino.jd_resource_nodes;

import jammingdino.jd_resource_nodes.block.ResourceNodeBlock;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.concurrent.CompletableFuture;

public class ResourceNodesBlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public ResourceNodesBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup arg) {
        // Loop through all our custom blocks
        for (ResourceNodeBlock node : ResourceNodes.REGISTERED_NODES) {

            // 1. All nodes are mineable with a pickaxe
            getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).add(node);

            // 2. Automate Mining Tier (Iron, Diamond, etc.)
            // We check what the Original Ore required, and copy that requirement.
            Block original = node.getOriginalOre();

            // Check Stone Tier
            if (original.getDefaultState().isIn(BlockTags.NEEDS_STONE_TOOL)) {
                getOrCreateTagBuilder(BlockTags.NEEDS_STONE_TOOL).add(node);
            }
            // Check Iron Tier
            else if (original.getDefaultState().isIn(BlockTags.NEEDS_IRON_TOOL)) {
                getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).add(node);
            }
            // Check Diamond Tier
            else if (original.getDefaultState().isIn(BlockTags.NEEDS_DIAMOND_TOOL)) {
                getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL).add(node);
            }

            // Note: If the original ore had no requirement (like Coal),
            // we don't add any specific tier tag, which is correct (Wooden pick works).
        }
    }
}