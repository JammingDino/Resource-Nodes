package jammingdino.jd_resource_nodes.block;

import jammingdino.jd_resource_nodes.ResourceNodeTier;
import jammingdino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

public class ResourceNodeBlock extends BlockWithEntity {
    public static final BooleanProperty REGENERATING = BooleanProperty.of("regenerating");

    private final Block originalOre;
    private final Block baseBlock;
    private final ResourceNodeTier tier;

    public ResourceNodeBlock(Block originalOre, Block baseBlock, ResourceNodeTier tier, Settings settings) {
        super(settings);
        this.originalOre = originalOre;
        this.baseBlock = baseBlock;
        this.tier = tier;
        this.setDefaultState(this.stateManager.getDefaultState().with(REGENERATING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(REGENERATING);
    }

    // --- Main Player Logic ---
    // Returns TRUE if the event should continue (block breaks permanently)
    // Returns FALSE if we handled it (block regenerates)
    public boolean handlePlayerBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.isClient) return false;

        BlockEntity be = world.getBlockEntity(pos);
        ResourceNodeBlockEntity nodeBe = (be instanceof ResourceNodeBlockEntity) ? (ResourceNodeBlockEntity) be : null;

        // 1. Creative Mode -> REMOVE
        if (player.isCreative()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            return true; // Let vanilla break logic happen
        }

        // 2. Silk Touch -> DROP NODE & REMOVE
        ItemStack heldItem = player.getMainHandStack();
        if (EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, heldItem) > 0) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            // Drop THIS block (The Resource Node)
            dropStack(world, pos, new ItemStack(this));
            return true; // Let vanilla break logic happen (to remove the block)
        }

        // 3. Crouching (Shift) -> DROP ORE & REMOVE
        if (player.isSneaking()) {
            if (nodeBe != null) nodeBe.setPermanentlyRemoved(true);
            // Drop the ORIGINAL ORE loot (e.g. Raw Iron)
            dropStacks(originalOre.getDefaultState(), world, pos, null, player, heldItem);
            return true; // Let vanilla break logic happen
        }

        // 4. Normal Mining -> REGENERATE
        // Drop the ore loot
        dropStacks(originalOre.getDefaultState(), world, pos, null, player, heldItem);
        // Switch to stone
        deplete(world, pos, state);
        return false; // Cancel the vanilla break event so it doesn't turn to air
    }

    // --- Helper to start regeneration ---
    public void deplete(World world, BlockPos pos, BlockState oldState) {
        world.setBlockState(pos, oldState.with(REGENERATING, true), 3);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ResourceNodeBlockEntity nodeBe) {
            nodeBe.activate(tier.getRegenerateTicks());
        }
    }

    // --- Machine / Explosion Logic ---
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient) {
            // Check if we are really being removed (turned to Air, Water, etc)
            if (newState.isAir() || !newState.isOf(this)) {

                // 1. Check if a player explicitly asked to remove this (via the flag)
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof ResourceNodeBlockEntity nodeBe) {
                    if (nodeBe.isPermanentlyRemoved()) {
                        super.onStateReplaced(state, world, pos, newState, moved);
                        return; // Let it die
                    }

                    // SAVE TIMER if it was Stone
                    int oldTimer = nodeBe.getTimer();

                    // 2. Machine/Explosion Logic (Revive the block)
                    boolean wasOre = !state.get(REGENERATING);

                    // A. Revive the Block
                    world.setBlockState(pos, state.with(REGENERATING, true), 3);

                    // B. Restore/Start Timer
                    BlockEntity newBe = world.getBlockEntity(pos);
                    if (newBe instanceof ResourceNodeBlockEntity newNodeBe) {
                        if (wasOre) {
                            // It was fresh ore -> Start full timer
                            newNodeBe.activate(tier.getRegenerateTicks());

                            // CRITICAL: Drop items for the machine!
                            // Machines usually break the block. Since we revived it, we must manually drop the items.
                            // We use originalOre's default state to simulate drops.
                            dropStacks(originalOre.getDefaultState(), (ServerWorld) world, pos, null, null, ItemStack.EMPTY);
                        } else {
                            // It was already stone -> Keep old timer
                            newNodeBe.activate(oldTimer > 0 ? oldTimer : tier.getRegenerateTicks());
                        }
                    }
                    return; // Prevent super.onStateReplaced from removing the BE
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // --- Explosion specific override ---
    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        // We handle logic in onStateReplaced, but to be safe against some mods:
        if (!world.isClient) {
            deplete(world, pos, this.getDefaultState());
        }
    }

    // --- Boilerplate ---
    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        if (state.get(REGENERATING)) return 0.0f;
        return super.calcBlockBreakingDelta(state, player, world, pos);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (state.get(REGENERATING)) {
            return checkType(type, jammingdino.jd_resource_nodes.ResourceNodes.RESOURCE_NODE_BLOCK_ENTITY, ResourceNodeBlockEntity::tick);
        }
        return null;
    }

    public Block getOriginalOre() { return originalOre; }
    public Block getBaseBlock() { return baseBlock; }
}