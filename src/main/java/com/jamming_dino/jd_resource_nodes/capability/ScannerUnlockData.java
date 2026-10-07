package com.jamming_dino.jd_resource_nodes.capability;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public class ScannerUnlockData {
    private static final String ROOT_KEY = "scanner_data";
    private static final String UNLOCKED_KEY = "Unlocked";

    private final Set<String> unlockedCategories = new HashSet<>();

    public static ScannerUnlockData get(Player player) {
        ScannerUnlockData data = new ScannerUnlockData();
        CompoundTag root = getOrCreatePersisted(player).getCompound(ROOT_KEY);
        data.deserializeNBT(root);
        return data;
    }

    public static void save(Player player, ScannerUnlockData data) {
        getOrCreatePersisted(player).put(ROOT_KEY, data.serializeNBT());
    }

    private static CompoundTag getOrCreatePersisted(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persisted = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        persistentData.put(Player.PERSISTED_NBT_TAG, persisted);
        return persisted;
    }

    public boolean unlock(String category) {
        return unlockedCategories.add(category);
    }

    public boolean lock(String category) {
        return unlockedCategories.remove(category);
    }

    public void unlockAll(Set<String> allCategories) {
        unlockedCategories.addAll(allCategories);
    }

    public void lockAll() {
        unlockedCategories.clear();
    }

    public boolean isUnlocked(String category) {
        return unlockedCategories.contains(category);
    }

    public Set<String> getUnlockedCategories() {
        return new HashSet<>(unlockedCategories);
    }

    // For Client Syncing
    public void setUnlockedCategories(Set<String> newSet) {
        this.unlockedCategories.clear();
        this.unlockedCategories.addAll(newSet);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String s : unlockedCategories) {
            list.add(StringTag.valueOf(s));
        }
        tag.put(UNLOCKED_KEY, list);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        unlockedCategories.clear();
        if (tag.contains(UNLOCKED_KEY)) {
            ListTag list = tag.getList(UNLOCKED_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                unlockedCategories.add(list.getString(i));
            }
        }
    }
}