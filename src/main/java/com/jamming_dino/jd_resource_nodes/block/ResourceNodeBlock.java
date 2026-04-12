package com.jamming_dino.jd_resource_nodes.block;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig; // Import Config
import com.jamming_dino.jd_resource_nodes.NodeActivationState;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList; // Added
import java.util.List;

public class ResourceNodeBlock extends Block implements EntityBlock {
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");

    private final Block originalOre;
    private final Block readyBlock;
    private final Block baseBlock;
    private final ResourceLocation outputItemId;
    private final ResourceNodeTier tier;
    private final String overlaySource;

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        Block target = state.getValue(REGENERATING) ? baseBlock : readyBlock;
        if (target != null && target != this) {
            return target.defaultBlockState().getDestroyProgress(player, level, pos);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    public ResourceNodeBlock(Block originalOre, Block readyBlock, Block baseBlock, ResourceLocation outputItemId, ResourceNodeTier tier, String overlaySource, Properties properties) {
        super(properties);
        this.originalOre = originalOre;
        this.readyBlock = readyBlock;
        this.baseBlock = baseBlock;
        this.outputItemId = outputItemId;
        this.tier = tier;
        this.overlaySource = overlaySource;
        this.registerDefaultState(this.stateDefinition.any().setValue(REGENERATING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REGENERATING);
    }

    // --- Main Player Logic ---
    public boolean handlePlayerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;

        if (!NodeActivationState.isNodeEnabled(level, this)) {
            // Disabled nodes should break with vanilla behavior.
            return true;
        }

        BlockEntity be = level.getBlockEntity(pos);
        ResourceNodeBlockEntity nodeBe = (be instanceof ResourceNodeBlockEntity) ? (ResourceNodeBlockEntity) be : null;

        // --- Stone State ---
        if (state.getValue(REGENERATING)) {
            if (player.isCreative() || player.isShiftKeyDown()) {
                if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
                return true;
            }
            return false;
        }

        // --- Ore State ---

        // 1. Creative Mode -> REMOVE
        if (player.isCreative()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            return true;
        }

        // 2. Silk Touch check
        ItemStack heldItem = player.getMainHandItem();
        boolean hasSilkTouch = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                heldItem
        ) > 0;

        if (hasSilkTouch) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(this));
            return true;
        }

        // 3. Crouching (Shift) -> DROP ORE & REMOVE
        if (player.isShiftKeyDown()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            // Just one drop for breaking the block itself
            dropConfiguredLoot(level, pos, 1);
            return true;
        }

        // 4. Normal Mining -> REGENERATE
        // Loop drops based on tier
        dropConfiguredLoot(level, pos, tier.getDropCount());
        deplete(level, pos, state);

        // Vanilla Tool Damage Logic
        heldItem.mineBlock(level, state, pos, player);

        return false; // Cancel event
    }

    private void dropConfiguredLoot(Level level, BlockPos pos, int amount) {
        Item outputItem = BuiltInRegistries.ITEM.get(outputItemId);
        if (outputItem == Items.AIR) {
            outputItem = Items.COBBLESTONE;
        }

        List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            drops.add(new ItemStack(outputItem));
        }
        spawnSmartDrops(level, pos, drops);
    }

    public void deplete(Level level, BlockPos pos, BlockState oldState) {
        level.setBlock(pos, oldState.setValue(REGENERATING, true), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ResourceNodeBlockEntity nodeBe) {
            // Use global config for time
            nodeBe.activate(ResourceNodesConfig.getRegenerateTicks());
        }
    }

    // --- Machine / Smart Drop Logic ---
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !NodeActivationState.isNodeEnabled(level, this)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        if (!level.isClientSide) {
            if (newState.isAir() || !newState.is(this)) {

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ResourceNodeBlockEntity nodeBe) {
                    if (nodeBe.isPermanentlyRemoved()) {
                        super.onRemove(state, level, pos, newState, isMoving);
                        return;
                    }

                    int oldTimer = nodeBe.getTimer();
                    boolean wasOre = !state.getValue(REGENERATING);

                    // A. Revive
                    level.setBlock(pos, state.setValue(REGENERATING, true), 3);

                    // B. Restore BE
                    BlockEntity newBe = level.getBlockEntity(pos);
                    if (newBe instanceof ResourceNodeBlockEntity newNodeBe) {
                        if (wasOre) {
                            // Reset timer to global config
                            newNodeBe.activate(ResourceNodesConfig.getRegenerateTicks());

                            // C. Machine Drops use configured output item.
                            dropConfiguredLoot(level, pos, tier.getDropCount());
                        } else {
                            // If it was already regenerating, keep the old timer
                            newNodeBe.activate(oldTimer > 0 ? oldTimer : ResourceNodesConfig.getRegenerateTicks());
                        }
                    }
                    return; // Stop removal
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // --- SHARED HELPER: Smart Drops ---
    private void spawnSmartDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        BlockPos belowPos = pos.below();
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, belowPos, Direction.UP);

        if (handler != null) {
            for (int i = 0; i < drops.size(); i++) {
                ItemStack stack = drops.get(i);
                if (stack.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                drops.set(i, remainder);
            }
        }

        BlockPos abovePos = pos.above();
        boolean isBelowFree = level.isEmptyBlock(belowPos) || level.getBlockState(belowPos).getCollisionShape(level, belowPos).isEmpty();
        boolean isAboveFree = level.isEmptyBlock(abovePos) || level.getBlockState(abovePos).getCollisionShape(level, abovePos).isEmpty();

        double spawnY;

        if (isBelowFree) spawnY = pos.getY() - 0.5;
        else if (isAboveFree) spawnY = pos.getY() + 1.2;
        else spawnY = pos.getY() + 0.5;

        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, spawnY, pos.getZ() + 0.5, stack);
            if (isBelowFree || isAboveFree) itemEntity.setDeltaMovement(0, 0, 0);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(REGENERATING)) {
            if (type == ResourceNodes.RESOURCE_NODE_BE.get()) {
                return (lvl, pos, st, be) -> ResourceNodeBlockEntity.tick(lvl, pos, st, (ResourceNodeBlockEntity) be);
            }
        }
        return null;
    }

    public Block getOriginalOre() { return originalOre; }
    public Block getReadyBlock() { return readyBlock; }
    public Block getBaseBlock() { return baseBlock; }
    
    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        // If it's a custom dynamic node (has no JSON model), we only want it rendered via BER.
        // For standard nodes, it has a JSON model so we can use MODEL.
        // Wait, how do we distinguish? Custom nodes start with "node_custom_"
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(this);
        if (id != null && id.getPath().startsWith("node_custom_")) {
            return net.minecraft.world.level.block.RenderShape.INVISIBLE;
        }
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    public Item getOutputItem() {
        Item outputItem = BuiltInRegistries.ITEM.get(outputItemId);
        return outputItem == Items.AIR ? Items.COBBLESTONE : outputItem;
    }
    public ResourceLocation getOutputItemId() { return outputItemId; }
    public ResourceNodeTier getTier() { return tier; }
    public String getOverlaySource() { return overlaySource; }
}
