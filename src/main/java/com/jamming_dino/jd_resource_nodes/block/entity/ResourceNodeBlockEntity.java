package com.jamming_dino.jd_resource_nodes.block.entity;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ResourceNodeBlockEntity extends BlockEntity {
    private int timer = 0;
    private boolean permanentlyRemoved = false;

    public ResourceNodeBlockEntity(BlockPos pos, BlockState state) {
        // We access the Registry Object via .get()
        // This line will show an error until we update ResourceNodes.java in Step 6
        super(ResourceNodes.RESOURCE_NODE_BE.get(), pos, state);
    }

    public void activate(int ticks) {
        this.timer = ticks;
    }

    public int getTimer() {
        return this.timer;
    }

    public void setPermanentlyRemoved(boolean removed) {
        this.permanentlyRemoved = removed;
    }

    public boolean isPermanentlyRemoved() {
        return permanentlyRemoved;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResourceNodeBlockEntity be) {
        if (level.isClientSide) return;

        if (be.timer > 0) {
            be.timer--;
            if (be.timer <= 0) {
                // When timer hits 0, turn off "Regenerating"
                level.setBlockAndUpdate(pos, state.setValue(ResourceNodeBlock.REGENERATING, false));
            }
        }
    }

    // --- NBT CHANGES FOR 1.21 ---
    // writeNbt -> saveAdditional
    // NbtCompound -> CompoundTag
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Timer", timer);
        // We don't save permanentlyRemoved because it's transient logic for breaking
    }

    // readNbt -> loadAdditional
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.timer = tag.getInt("Timer");
    }
}