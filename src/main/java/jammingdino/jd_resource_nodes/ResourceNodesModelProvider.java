package jammingdino.jd_resource_nodes;

import jammingdino.jd_resource_nodes.block.ResourceNodeBlock;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;

public class ResourceNodesModelProvider extends FabricModelProvider {
    public ResourceNodesModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        // Iterate over every node we registered in the main class
        for (ResourceNodeBlock nodeBlock : ResourceNodes.REGISTERED_NODES) {

            // 1. Define the Visual Logic
            generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(nodeBlock)
                    .coordinate(BlockStateVariantMap.create(ResourceNodeBlock.REGENERATING)
                            // If regenerating is false, use the model of the Original Ore (e.g. Iron Ore)
                            .register(false, BlockStateVariant.create()
                                    .put(VariantSettings.MODEL, ModelIds.getBlockModelId(nodeBlock.getOriginalOre())))
                            // If regenerating is true, use the model of the Base Block (e.g. Stone)
                            .register(true, BlockStateVariant.create()
                                    .put(VariantSettings.MODEL, ModelIds.getBlockModelId(nodeBlock.getBaseBlock())))
                    )
            );

            // 2. Generate the Item Model (So it looks like the ore in your inventory)
            // We parent it to the Original Ore's model
            generator.registerParentedItemModel(nodeBlock, ModelIds.getBlockModelId(nodeBlock.getOriginalOre()));
        }
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        // We handled the block items in generateBlockStateModels, so this can be empty
    }
}