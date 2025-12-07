package com.jamming_dino.jd_resource_nodes;

import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import com.jamming_dino.jd_resource_nodes.client.ResourceNodesKeys;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.jamming_dino.jd_resource_nodes.datagen.ResourceNodesDataGen;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mod(ResourceNodes.MODID)
public class ResourceNodes {
    public static final String MODID = "jd_resource_nodes";
    public static final Logger LOGGER = LogUtils.getLogger();

    // --- Registries ---
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // --- Storage for DataGen and Logic ---
    // We store the DeferredBlock references here so we can access them later (e.g. for the BlockEntity or Tag Generation)
    public static final List<DeferredBlock<ResourceNodeBlock>> REGISTERED_NODES = new ArrayList<>();

    // --- Static Initialization of Nodes ---
    // This runs as soon as the class is loaded, populating the BLOCKS and ITEMS registries before they are registered to the bus.
    static {
        registerNodeSet("coal", Blocks.COAL_ORE, Blocks.STONE);
        registerNodeSet("deepslate_coal", Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE);

        registerNodeSet("copper", Blocks.COPPER_ORE, Blocks.STONE);
        registerNodeSet("deepslate_copper", Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE);

        registerNodeSet("iron", Blocks.IRON_ORE, Blocks.STONE);
        registerNodeSet("deepslate_iron", Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE);

        registerNodeSet("gold", Blocks.GOLD_ORE, Blocks.STONE);
        registerNodeSet("deepslate_gold", Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE);
        registerNodeSet("nether_gold", Blocks.NETHER_GOLD_ORE, Blocks.NETHERRACK);

        registerNodeSet("redstone", Blocks.REDSTONE_ORE, Blocks.STONE);
        registerNodeSet("deepslate_redstone", Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE);

        registerNodeSet("lapis", Blocks.LAPIS_ORE, Blocks.STONE);
        registerNodeSet("deepslate_lapis", Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE);

        registerNodeSet("emerald", Blocks.EMERALD_ORE, Blocks.STONE);
        registerNodeSet("deepslate_emerald", Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE);

        registerNodeSet("diamond", Blocks.DIAMOND_ORE, Blocks.STONE);
        registerNodeSet("deepslate_diamond", Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE);

        registerNodeSet("nether_quartz", Blocks.NETHER_QUARTZ_ORE, Blocks.NETHERRACK);
        registerNodeSet("ancient_debris", Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK);
    }

    // --- Block Entity Registration ---
    // We define this AFTER the static block so REGISTERED_NODES is full.
    // .get() on DeferredBlock returns the actual Block instance during registry phase.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceNodeBlockEntity>> RESOURCE_NODE_BE =
            BLOCK_ENTITIES.register("resource_node_be", () ->
                    BlockEntityType.Builder.of(
                            ResourceNodeBlockEntity::new,
                            REGISTERED_NODES.stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null)
            );

    // --- Creative Tab ---
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("general", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID + ".general"))
            .icon(() -> new ItemStack(Items.IRON_PICKAXE))
            .displayItems((parameters, output) -> {
                // Automatically add every registered node item to the tab
                for (DeferredBlock<ResourceNodeBlock> block : REGISTERED_NODES) {
                    output.accept(block);
                }
            })
            .build());

    // --- Constructor ---
    public ResourceNodes(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        ResourceNodesConfig.load();
        LOGGER.info("Initializing Resource Nodes (NeoForge 1.21)...");

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // REGISTER DATAGEN HERE
        modEventBus.addListener(ResourceNodesDataGen::gatherData);
        modEventBus.addListener(ResourceNodesKeys::registerKeys);
    }

    // --- Helper Methods ---

    private static void registerNodeSet(String baseName, Block originalOre, Block baseBlock) {
        for (ResourceNodeTier tier : ResourceNodeTier.values()) {
            String regName = "node_" + baseName + "_" + tier.getSerializedName();
            registerNode(regName, originalOre, baseBlock, tier);
        }
    }

    private static void registerNode(String name, Block originalOre, Block baseBlock, ResourceNodeTier tier) {
        // Create the Block Supplier
        Supplier<ResourceNodeBlock> blockSupplier = () -> new ResourceNodeBlock(
                originalOre,
                baseBlock,
                tier,
                // In 1.21, we use ofFullCopy to copy settings
                BlockBehaviour.Properties.ofFullCopy(originalOre)
                        .lightLevel(state -> 0) // Override light level
        );

        // Register Block
        DeferredBlock<ResourceNodeBlock> registeredBlock = BLOCKS.register(name, blockSupplier);

        // Register BlockItem
        DeferredItem<BlockItem> registeredItem = ITEMS.registerSimpleBlockItem(name, registeredBlock);

        // Add to our list for BE and DataGen
        REGISTERED_NODES.add(registeredBlock);
    }
}