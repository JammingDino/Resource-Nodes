package jammingdino.jd_resource_nodes;

import jammingdino.jd_resource_nodes.block.ResourceNodeBlock;
import jammingdino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ResourceNodes implements ModInitializer {
    public static final String MOD_ID = "jd_resource_nodes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static BlockEntityType<ResourceNodeBlockEntity> RESOURCE_NODE_BLOCK_ENTITY;
    public static final List<ResourceNodeBlock> REGISTERED_NODES = new ArrayList<>();
    public static final RegistryKey<ItemGroup> RESOURCE_NODES_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP, new Identifier(MOD_ID, "general"));

    @Override
    public void onInitialize() {
        ResourceNodesConfig.load();
        LOGGER.info("Initializing Resource Nodes...");

        // 1. Register Group
        Registry.register(Registries.ITEM_GROUP, RESOURCE_NODES_GROUP, FabricItemGroup.builder()
                .icon(() -> new ItemStack(Items.IRON_PICKAXE))
                .displayName(Text.translatable("itemGroup." + MOD_ID + ".general"))
                .build());

        // 2. Register Node Sets (Impure, Normal, Pure for each)

        // --- Coal ---
        registerNodeSet("coal", Blocks.COAL_ORE, Blocks.STONE);
        registerNodeSet("deepslate_coal", Blocks.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE);

        // --- Copper ---
        registerNodeSet("copper", Blocks.COPPER_ORE, Blocks.STONE);
        registerNodeSet("deepslate_copper", Blocks.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE);

        // --- Iron ---
        registerNodeSet("iron", Blocks.IRON_ORE, Blocks.STONE);
        registerNodeSet("deepslate_iron", Blocks.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE);

        // --- Gold ---
        registerNodeSet("gold", Blocks.GOLD_ORE, Blocks.STONE);
        registerNodeSet("deepslate_gold", Blocks.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE);
        registerNodeSet("nether_gold", Blocks.NETHER_GOLD_ORE, Blocks.NETHERRACK);

        // --- Redstone ---
        registerNodeSet("redstone", Blocks.REDSTONE_ORE, Blocks.STONE);
        registerNodeSet("deepslate_redstone", Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE);

        // --- Lapis Lazuli ---
        registerNodeSet("lapis", Blocks.LAPIS_ORE, Blocks.STONE);
        registerNodeSet("deepslate_lapis", Blocks.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE);

        // --- Emerald ---
        registerNodeSet("emerald", Blocks.EMERALD_ORE, Blocks.STONE);
        registerNodeSet("deepslate_emerald", Blocks.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE);

        // --- Diamond ---
        registerNodeSet("diamond", Blocks.DIAMOND_ORE, Blocks.STONE);
        registerNodeSet("deepslate_diamond", Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE);

        // --- Nether Quartz ---
        registerNodeSet("nether_quartz", Blocks.NETHER_QUARTZ_ORE, Blocks.NETHERRACK);

        // --- Ancient Debris ---
        // Technically not an "ore" block class, but functions identically for this mod
        registerNodeSet("ancient_debris", Blocks.ANCIENT_DEBRIS, Blocks.NETHERRACK);

        // 3. Register Block Entity
        RESOURCE_NODE_BLOCK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(MOD_ID, "resource_node_be"),
                FabricBlockEntityTypeBuilder.create(ResourceNodeBlockEntity::new,
                                REGISTERED_NODES.toArray(new Block[0]))
                        .build()
        );

        // 4. Register Break Event
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (state.getBlock() instanceof ResourceNodeBlock nodeBlock) {
                // Call our logic.
                // If it returns TRUE, we allow the vanilla break (Removal).
                // If it returns FALSE, we cancel the event (Regeneration).
                return nodeBlock.handlePlayerBreak(world, pos, state, player);
            }
            return true;
        });
    }

    /**
     * Automatically registers Impure, Normal, and Pure versions of an ore
     */
    private void registerNodeSet(String baseName, Block originalOre, Block baseBlock) {
        for (ResourceNodeTier tier : ResourceNodeTier.values()) {
            // Name format: node_iron_impure, node_iron_normal, node_iron_pure
            String regName = "node_" + baseName + "_" + tier.asString();

            registerNode(regName, originalOre, baseBlock, tier);
        }
    }

    private void registerNode(String name, Block originalOre, Block baseBlock, ResourceNodeTier tier) {
        // Change type from 'FabricBlockSettings' to 'AbstractBlock.Settings'
        // This prevents the ClassCastException
        AbstractBlock.Settings settings = FabricBlockSettings.copy(originalOre)
                .luminance(state -> 0);

        // The constructor accepts AbstractBlock.Settings, so this works perfectly
        ResourceNodeBlock block = new ResourceNodeBlock(
                originalOre,
                baseBlock,
                tier,
                settings
        );

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, name), block);
        BlockItem blockItem = new BlockItem(block, new FabricItemSettings());
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), blockItem);
        net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents.modifyEntriesEvent(RESOURCE_NODES_GROUP).register(content -> content.add(blockItem));
        REGISTERED_NODES.add(block);
    }
}