package com.jamming_dino.jd_resource_nodes.block;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ResourceNodeBlock extends Block implements EntityBlock {
    public static final BooleanProperty REGENERATING = BooleanProperty.create("regenerating");

    private final Block originalOre; // Can be Blocks.AIR for custom ores
    private final Block baseBlock; // Stone, Tuff, or Deepslate
    private final ResourceNodeTier tier;
    private final Supplier<? extends Item> outputItem;

    public ResourceNodeBlock(Block originalOre, Block baseBlock, ResourceNodeTier tier, Supplier<? extends Item> outputItem, Properties properties) {
        super(properties);
        this.originalOre = originalOre;
        this.baseBlock = baseBlock;
        this.tier = tier;
        this.outputItem = outputItem;
        this.registerDefaultState(this.stateDefinition.any().setValue(REGENERATING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REGENERATING);
    }

    public boolean handlePlayerBreak(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;

        BlockEntity be = level.getBlockEntity(pos);
        ResourceNodeBlockEntity nodeBe = (be instanceof ResourceNodeBlockEntity) ? (ResourceNodeBlockEntity) be : null;

        // --- Regenerating State (Stone/Tuff/Deepslate) ---
        if (state.getValue(REGENERATING)) {
            if (player.isCreative() || player.isShiftKeyDown()) {
                if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
                return true;
            }
            return false;
        }

        // --- Ore State ---
        if (player.isCreative()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            return true;
        }

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

        if (player.isShiftKeyDown()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            generateAndSpawnDrops(level, pos, heldItem, 1);
            return true;
        }

        // Normal Mining
        generateAndSpawnDrops(level, pos, heldItem, tier.getDropCount());
        deplete(level, pos, state);
        heldItem.mineBlock(level, state, pos, player);

        return false;
    }

    private void generateAndSpawnDrops(Level level, BlockPos pos, ItemStack tool, int multiplier) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        List<ItemStack> baseDrops;

        // 1. Determine base drops
        if (originalOre != null && originalOre != Blocks.AIR) {
            // Vanilla-style: ask the vanilla block what it drops
            BlockState oreState = originalOre.defaultBlockState();
            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                    .withParameter(LootContextParams.TOOL, tool)
                    .withParameter(LootContextParams.BLOCK_STATE, oreState);
            baseDrops = oreState.getDrops(builder);
        } else {
            // Custom-style: drop the defined item
            // Basic fortune logic could be added here manually if desired
            baseDrops = Collections.singletonList(new ItemStack(outputItem.get()));
        }

        // 2. Multiply
        List<ItemStack> finalDrops = new ArrayList<>();
        for (int i = 0; i < multiplier; i++) {
            for (ItemStack stack : baseDrops) {
                finalDrops.add(stack.copy());
            }
        }

        // 3. Spawn
        spawnSmartDrops(level, pos, finalDrops);
    }

    public void deplete(Level level, BlockPos pos, BlockState oldState) {
        level.setBlock(pos, oldState.setValue(REGENERATING, true), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ResourceNodeBlockEntity nodeBe) {
            nodeBe.activate(ResourceNodesConfig.getRegenerateTicks());
        }
    }

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
                    boolean wasOre = !state.getValue(REGENERATING);
                    int oldTimer = nodeBe.getTimer();

                    level.setBlock(pos, state.setValue(REGENERATING, true), 3);

                    BlockEntity newBe = level.getBlockEntity(pos);
                    if (newBe instanceof ResourceNodeBlockEntity newNodeBe) {
                        if (wasOre) {
                            newNodeBe.activate(ResourceNodesConfig.getRegenerateTicks());
                            generateAndSpawnDrops(level, pos, ItemStack.EMPTY, tier.getDropCount());
                        } else {
                            newNodeBe.activate(oldTimer > 0 ? oldTimer : ResourceNodesConfig.getRegenerateTicks());
                        }
                    }
                    return;
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

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

        double spawnY = isBelowFree ? pos.getY() - 0.5 : (isAboveFree ? pos.getY() + 1.2 : pos.getY() + 0.5);

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
    public Block getBaseBlock() { return baseBlock; }
}