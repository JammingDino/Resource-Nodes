package com.jamming_dino.jd_resource_nodes;

import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import com.jamming_dino.jd_resource_nodes.client.ResourceNodesKeys;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.attachment.IAttachmentHolder; // ADDED THIS IMPORT
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.jamming_dino.jd_resource_nodes.datagen.ResourceNodesDataGen;
import org.jetbrains.annotations.Nullable;
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
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    // --- Storage for DataGen and Logic ---
    public static final List<DeferredBlock<ResourceNodeBlock>> REGISTERED_NODES = new ArrayList<>();

    // Storage for linking categories to deferred blocks (resolved later)
    private static final List<CategoryRegistration> PENDING_CATEGORY_REGISTRATIONS = new ArrayList<>();

    private static class CategoryRegistration {
        final String category;
        final DeferredBlock<ResourceNodeBlock> block;

        CategoryRegistration(String category, DeferredBlock<ResourceNodeBlock> block) {
            this.category = category;
            this.block = block;
        }
    }

    // --- Static Initialization of Nodes ---
    static {
        // Coal nodes - output coal (group all variants together)
        registerNodeSet("coal", Blocks.COAL_ORE, Blocks.STONE, Items.COAL, "coal");
        registerNodeSet("deepslate_coal", Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE, Items.COAL, "coal");

        // Copper nodes - output raw copper
        registerNodeSet("copper", Blocks.COPPER_ORE, Blocks.STONE, Items.RAW_COPPER, "copper");
        registerNodeSet("deepslate_copper", Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE, Items.RAW_COPPER, "copper");

        // Iron nodes - output raw iron
        registerNodeSet("iron", Blocks.IRON_ORE, Blocks.STONE, Items.RAW_IRON, "iron");
        registerNodeSet("deepslate_iron", Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE, Items.RAW_IRON, "iron");

        // Gold nodes - output raw gold
        registerNodeSet("gold", Blocks.GOLD_ORE, Blocks.STONE, Items.RAW_GOLD, "gold");
        registerNodeSet("deepslate_gold", Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE, Items.RAW_GOLD, "gold");
        registerNodeSet("nether_gold", Blocks.NETHER_GOLD_ORE, Blocks.NETHERRACK, Items.RAW_GOLD, "gold");

        // Redstone nodes - output redstone dust
        registerNodeSet("redstone", Blocks.REDSTONE_ORE, Blocks.STONE, Items.REDSTONE, "redstone");
        registerNodeSet("deepslate_redstone", Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE, Items.REDSTONE, "redstone");

        // Lapis nodes - output lapis lazuli
        registerNodeSet("lapis", Blocks.LAPIS_ORE, Blocks.STONE, Items.LAPIS_LAZULI, "lapis");
        registerNodeSet("deepslate_lapis", Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE, Items.LAPIS_LAZULI, "lapis");

        // Emerald nodes - output emerald
        registerNodeSet("emerald", Blocks.EMERALD_ORE, Blocks.STONE, Items.EMERALD, "emerald");
        registerNodeSet("deepslate_emerald", Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE, Items.EMERALD, "emerald");

        // Diamond nodes - output diamond
        registerNodeSet("diamond", Blocks.DIAMOND_ORE, Blocks.STONE, Items.DIAMOND, "diamond");
        registerNodeSet("deepslate_diamond", Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE, Items.DIAMOND, "diamond");

        // Nether Quartz nodes - output quartz
        registerNodeSet("nether_quartz", Blocks.NETHER_QUARTZ_ORE, Blocks.NETHERRACK, Items.QUARTZ, "quartz");

        // Ancient Debris nodes - output netherite scrap
        registerNodeSet("ancient_debris", Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK, Items.NETHERITE_SCRAP, "ancient_debris");
    }

    // --- Attachment Registration ---
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ScannerUnlockData>> SCANNER_DATA = ATTACHMENT_TYPES.register(
            "scanner_data", () -> AttachmentType.builder(ScannerUnlockData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, ScannerUnlockData>() {
                        // FIXED: Added IAttachmentHolder holder parameter
                        @Override
                        public ScannerUnlockData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            ScannerUnlockData data = new ScannerUnlockData();
                            data.deserializeNBT(provider, tag);
                            return data;
                        }

                        @Override
                        public @Nullable CompoundTag write(ScannerUnlockData data, HolderLookup.Provider provider) {
                            return data.serializeNBT(provider);
                        }
                    })
                    .build()
    );

    // --- Block Entity Registration ---
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
        ATTACHMENT_TYPES.register(modEventBus);

        // REGISTER DATAGEN HERE
        modEventBus.addListener(ResourceNodesDataGen::gatherData);
        modEventBus.addListener(ResourceNodesKeys::registerKeys);

        // Register Packet Handler
        modEventBus.addListener(com.jamming_dino.jd_resource_nodes.network.ResourceNodesPacketHandler::register);

        // Link deferred blocks to categories after registration
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            event.enqueueWork(() -> {
                for (CategoryRegistration reg : PENDING_CATEGORY_REGISTRATIONS) {
                    ResourceNodeData data = ResourceNodeData.getByCategory(reg.category);
                    if (data != null) {
                        data.addNode(reg.block.get());
                    }
                }
                PENDING_CATEGORY_REGISTRATIONS.clear();
            });
        });
    }

    // --- Helper Methods ---

    /**
     * Register a complete set of nodes (all tiers) with their output item
     */
    private static void registerNodeSet(String baseName, Block originalOre, Block baseBlock, Item outputItem, String categoryName) {
        // Get or create the category data for this node set
        ResourceNodeData categoryData = ResourceNodeData.getByCategory(categoryName);
        if (categoryData == null) {
            categoryData = ResourceNodeData.register(
                    categoryName,
                    outputItem,
                    new ItemStack(outputItem)
            );
        }

        // Register all tiers for this node set
        for (ResourceNodeTier tier : ResourceNodeTier.values()) {
            String regName = "node_" + baseName + "_" + tier.getSerializedName();
            DeferredBlock<ResourceNodeBlock> registeredBlock = registerNode(regName, originalOre, baseBlock, tier);

            // Store the category-block link for later resolution
            PENDING_CATEGORY_REGISTRATIONS.add(new CategoryRegistration(categoryName, registeredBlock));
        }
    }

    private static DeferredBlock<ResourceNodeBlock> registerNode(String name, Block originalOre, Block baseBlock, ResourceNodeTier tier) {
        // Create the Block Supplier
        Supplier<ResourceNodeBlock> blockSupplier = () -> new ResourceNodeBlock(
                originalOre,
                baseBlock,
                tier,
                BlockBehaviour.Properties.ofFullCopy(originalOre)
                        .lightLevel(state -> 0)
        );

        // Register Block
        DeferredBlock<ResourceNodeBlock> registeredBlock = BLOCKS.register(name, blockSupplier);

        // Register BlockItem
        DeferredItem<BlockItem> registeredItem = ITEMS.registerSimpleBlockItem(name, registeredBlock);

        // Add to our list for BE and DataGen
        REGISTERED_NODES.add(registeredBlock);

        return registeredBlock;
    }
}