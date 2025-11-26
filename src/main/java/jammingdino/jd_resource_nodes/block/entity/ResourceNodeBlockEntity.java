package jammingdino.jd_resource_nodes.block.entity;

import jammingdino.jd_resource_nodes.ResourceNodes;
import jammingdino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ResourceNodeBlockEntity extends BlockEntity {
    private int timer = 0;

    // New Flag: Transient (not saved to NBT)
    private boolean permanentlyRemoved = false;

    public ResourceNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ResourceNodes.RESOURCE_NODE_BLOCK_ENTITY, pos, state);
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

    public static void tick(World world, BlockPos pos, BlockState state, ResourceNodeBlockEntity be) {
        if (world.isClient) return;

        if (be.timer > 0) {
            be.timer--;
            if (be.timer <= 0) {
                world.setBlockState(pos, state.with(ResourceNodeBlock.REGENERATING, false));
            }
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("Timer", timer);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.timer = nbt.getInt("Timer");
    }
}