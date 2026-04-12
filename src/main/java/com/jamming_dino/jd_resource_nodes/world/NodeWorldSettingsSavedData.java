package com.jamming_dino.jd_resource_nodes.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public class NodeWorldSettingsSavedData extends SavedData {
    private static final String DATA_ID = "jd_resource_nodes_world_settings";
    private static final String DISABLED_KEY = "DisabledNodes";

    private final Set<String> disabledNodes = new HashSet<>();

    public static NodeWorldSettingsSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(NodeWorldSettingsSavedData::new, NodeWorldSettingsSavedData::load),
                DATA_ID
        );
    }

    private static NodeWorldSettingsSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        NodeWorldSettingsSavedData data = new NodeWorldSettingsSavedData();
        ListTag list = tag.getList(DISABLED_KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            data.disabledNodes.add(list.getString(i));
        }
        return data;
    }

    public boolean isEnabled(String blockId) {
        return !disabledNodes.contains(blockId);
    }

    public Set<String> getDisabledNodes() {
        return new HashSet<>(disabledNodes);
    }

    public void setEnabled(String blockId, boolean enabled) {
        if (enabled) {
            disabledNodes.remove(blockId);
        } else {
            disabledNodes.add(blockId);
        }
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (String id : disabledNodes) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(DISABLED_KEY, list);
        return tag;
    }
}

