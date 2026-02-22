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
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
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

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final List<DeferredBlock<ResourceNodeBlock>> REGISTERED_NODES = new ArrayList<>();

    // --- STORAGE FOR DELAYED REGISTRATION ---
    private static final List<CategoryRegistration> PENDING_CATEGORY_LINKS = new ArrayList<>();
    private static final List<PendingCategory> PENDING_CATEGORIES = new ArrayList<>();

    private record PendingCategory(String name, Supplier<? extends Item> iconItem) {}

    private static class CategoryRegistration {
        final String category;
        final DeferredBlock<ResourceNodeBlock> block;
        CategoryRegistration(String category, DeferredBlock<ResourceNodeBlock> block) {
            this.category = category;
            this.block = block;
        }
    }

    // --- 1. Register New Items (Custom Resources) ---
    public static final DeferredItem<Item> BAUXITE = ITEMS.register("bauxite", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIMESTONE = ITEMS.register("limestone", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SAM = ITEMS.register("sam", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SULFUR = ITEMS.register("sulfur", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> URANIUM = ITEMS.register("uranium", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CATERIUM = ITEMS.register("caterium", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RAW_QUARTZ = ITEMS.register("raw_quartz", () -> new Item(new Item.Properties()));

    // --- 2. Define Node Sets ---
    static {
        // -- Vanilla Resources --
        registerNodeSet("copper", Blocks.COPPER_ORE, Items.RAW_COPPER, "copper");
        registerNodeSet("iron", Blocks.IRON_ORE, Items.RAW_IRON, "iron");
        registerNodeSet("gold", Blocks.GOLD_ORE, Items.RAW_GOLD, "gold");

        // -- Custom Resources --
        registerCustomNodeSet("bauxite", BAUXITE, "bauxite");
        registerCustomNodeSet("limestone", LIMESTONE, "limestone");
        registerCustomNodeSet("sam", SAM, "sam");
        registerCustomNodeSet("sulfur", SULFUR, "sulfur");
        registerCustomNodeSet("uranium", URANIUM, "uranium");
        registerCustomNodeSet("caterium", CATERIUM, "caterium");
        registerCustomNodeSet("quartz", RAW_QUARTZ, "quartz");
    }

    // --- Standard Init Logic ---
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ScannerUnlockData>> SCANNER_DATA = ATTACHMENT_TYPES.register(
            "scanner_data", () -> AttachmentType.builder(ScannerUnlockData::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, ScannerUnlockData>() {
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
                    }).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceNodeBlockEntity>> RESOURCE_NODE_BE =
            BLOCK_ENTITIES.register("resource_node_be", () ->
                    BlockEntityType.Builder.of(ResourceNodeBlockEntity::new,
                            REGISTERED_NODES.stream().map(DeferredBlock::get).toArray(Block[]::new)
                    ).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("general", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MODID + ".general"))
            .icon(() -> new ItemStack(Items.IRON_PICKAXE))
            .displayItems((parameters, output) -> {
                for (DeferredBlock<ResourceNodeBlock> block : REGISTERED_NODES) output.accept(block);
                output.accept(BAUXITE.get());
                output.accept(LIMESTONE.get());
                output.accept(SAM.get());
                output.accept(SULFUR.get());
                output.accept(URANIUM.get());
                output.accept(CATERIUM.get());
                output.accept(RAW_QUARTZ.get());
            }).build());

    public ResourceNodes(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        ResourceNodesConfig.load();
        LOGGER.info("Initializing Resource Nodes Polarrt Dev Branch...");

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);

        modEventBus.addListener(ResourceNodesDataGen::gatherData);
        modEventBus.addListener(ResourceNodesKeys::registerKeys);
        modEventBus.addListener(com.jamming_dino.jd_resource_nodes.network.ResourceNodesPacketHandler::register);

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (PendingCategory pc : PENDING_CATEGORIES) {
                if (ResourceNodeData.getByCategory(pc.name) == null) {
                    Item item = pc.iconItem.get();
                    ResourceNodeData.register(pc.name, item, new ItemStack(item));
                }
            }
            PENDING_CATEGORIES.clear();

            for (CategoryRegistration reg : PENDING_CATEGORY_LINKS) {
                ResourceNodeData data = ResourceNodeData.getByCategory(reg.category);
                if (data != null) {
                    data.addNode(reg.block.get());
                }
            }
            PENDING_CATEGORY_LINKS.clear();
        });
    }

    private static void registerNodeSet(String baseName, Block vanillaDropSource, Item outputItem, String categoryName) {
        registerCustomNodeSet(baseName, () -> outputItem, categoryName);
    }

    private static void registerCustomNodeSet(String baseName, Supplier<? extends Item> outputItemSup, String categoryName) {
        PENDING_CATEGORIES.add(new PendingCategory(categoryName, outputItemSup));

        for (ResourceNodeTier tier : ResourceNodeTier.values()) {
            Block baseBlock = getBaseBlockForTier(tier);
            String regName = "node_" + baseName + "_" + tier.getSerializedName();

            DeferredBlock<ResourceNodeBlock> registeredBlock = registerNodeBlock(regName, Blocks.AIR, baseBlock, tier, outputItemSup);
            PENDING_CATEGORY_LINKS.add(new CategoryRegistration(categoryName, registeredBlock));
        }
    }

    private static Block getBaseBlockForTier(ResourceNodeTier tier) {
        return switch (tier) {
            case IMPURE -> Blocks.STONE;
            case NORMAL -> Blocks.TUFF;
            case PURE -> Blocks.DEEPSLATE;
        };
    }

    // --- UPDATED BLOCK PROPERTIES ---
    private static DeferredBlock<ResourceNodeBlock> registerNodeBlock(String name, Block originalOre, Block baseBlock, ResourceNodeTier tier, Supplier<? extends Item> outputItem) {

        // Dynamic Hardness: Deepslate is harder than Stone
        float hardness = (baseBlock == Blocks.DEEPSLATE) ? 4.5F : 3.0F;

        Supplier<ResourceNodeBlock> blockSupplier = () -> new ResourceNodeBlock(
                originalOre,
                baseBlock,
                tier,
                outputItem,
                BlockBehaviour.Properties.ofFullCopy(baseBlock)
                        .lightLevel(state -> 0)
                        .strength(hardness, 3.0f) // 4.5f for deepslate, 3.0f for others
                        .requiresCorrectToolForDrops() // MUST have pickaxe to drop
        );
        DeferredBlock<ResourceNodeBlock> registeredBlock = BLOCKS.register(name, blockSupplier);
        ITEMS.registerSimpleBlockItem(name, registeredBlock);
        REGISTERED_NODES.add(registeredBlock);
        return registeredBlock;
    }
}