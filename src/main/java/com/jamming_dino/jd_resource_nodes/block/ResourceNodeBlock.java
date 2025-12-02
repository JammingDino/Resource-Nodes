package com.jamming_dino.jd_resource_nodes.block;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourceNodeBlock extends Block implements EntityBlock {
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");

    private final Block originalOre;
    private final Block baseBlock;
    private final ResourceNodeTier tier;

    public ResourceNodeBlock(Block originalOre, Block baseBlock, ResourceNodeTier tier, Properties properties) {
        super(properties);
        this.originalOre = originalOre;
        this.baseBlock = baseBlock;
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(REGENERATING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REGENERATING);
    }

    // --- Main Player Logic ---
    public boolean handlePlayerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;

        BlockEntity be = level.getBlockEntity(pos);
        ResourceNodeBlockEntity nodeBe = (be instanceof ResourceNodeBlockEntity) ? (ResourceNodeBlockEntity) be : null;

        // --- NEW: Check if it is currently regenerating (Stone state) ---
        if (state.getValue(REGENERATING)) {
            // If it's stone, we only allow breaking if the player specifically wants to remove it
            // (Creative Mode or Sneaking)
            if (player.isCreative() || player.isShiftKeyDown()) {
                if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
                return true; // Allow vanilla break (drops Cobblestone/Base block)
            }

            // If they are just hitting the Stone with a pickaxe, DO NOTHING.
            // Do not drop items. Do not damage tool. Do not reset timer.
            return false; // Cancel event
        }

        // --- Below is Logic for when it is ORE ---

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
            // Drop THIS block
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(this));
            return true;
        }

        // 3. Crouching (Shift) -> DROP ORE & REMOVE
        if (player.isShiftKeyDown()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            dropOreLoot(level, pos, heldItem);
            return true;
        }

        // 4. Normal Mining -> REGENERATE
        dropOreLoot(level, pos, heldItem);
        deplete(level, pos, state);

        // Vanilla Tool Damage Logic
        heldItem.mineBlock(level, state, pos, player);

        return false; // Cancel event
    }

    private void dropOreLoot(Level level, BlockPos pos, ItemStack tool) {
        if (level instanceof ServerLevel serverLevel) {
            BlockState oreState = originalOre.defaultBlockState();

            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                    .withParameter(LootContextParams.TOOL, tool)
                    .withParameter(LootContextParams.BLOCK_STATE, oreState);

            List<ItemStack> drops = oreState.getDrops(builder);

            // Use shared logic
            spawnSmartDrops(level, pos, drops);
        }
    }

    public void deplete(Level level, BlockPos pos, BlockState oldState) {
        level.setBlock(pos, oldState.setValue(REGENERATING, true), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ResourceNodeBlockEntity nodeBe) {
            nodeBe.activate(tier.getRegenerateTicks());
        }
    }

    // --- Machine / Smart Drop Logic ---
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
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
                            newNodeBe.activate(tier.getRegenerateTicks());

                            // C. Machine Drops (No Tool)
                            if (level instanceof ServerLevel serverLevel) {
                                BlockState oreState = originalOre.defaultBlockState();
                                LootParams.Builder builder = new LootParams.Builder(serverLevel)
                                        .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                        .withParameter(LootContextParams.BLOCK_STATE, oreState);

                                List<ItemStack> drops = oreState.getDrops(builder);

                                // Use shared logic
                                spawnSmartDrops(level, pos, drops);
                            }
                        } else {
                            newNodeBe.activate(oldTimer > 0 ? oldTimer : tier.getRegenerateTicks());
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
        BlockPos abovePos = pos.above();

        // Check 1: Is the block Air?
        // Check 2: If not Air, is the collision shape empty (e.g. Grass, Water)?
        boolean isBelowFree = level.isEmptyBlock(belowPos) || level.getBlockState(belowPos).getCollisionShape(level, belowPos).isEmpty();
        boolean isAboveFree = level.isEmptyBlock(abovePos) || level.getBlockState(abovePos).getCollisionShape(level, abovePos).isEmpty();

        double spawnY;

        if (isBelowFree) {
            // PREFERRED: Drop below
            spawnY = pos.getY() - 0.5;
        } else if (isAboveFree) {
            // SECONDARY: Drop above
            spawnY = pos.getY() + 1.2;
        } else {
            // FALLBACK: Inside
            spawnY = pos.getY() + 0.5;
        }

        for (ItemStack stack : drops) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, spawnY, pos.getZ() + 0.5, stack);

            // If we found a free spot, kill velocity so it doesn't drift
            if (isBelowFree || isAboveFree) {
                itemEntity.setDeltaMovement(0, 0, 0);
            }

            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    // --- EntityBlock Implementation ---

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
    public Block getBaseBlock() { return baseBlock; }
}