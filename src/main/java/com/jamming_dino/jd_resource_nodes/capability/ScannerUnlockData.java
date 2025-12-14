package com.jamming_dino.jd_resource_nodes.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

public class ScannerUnlockData implements INBTSerializable<CompoundTag> {
    private final Set<String> unlockedCategories = new HashSet<>();

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

    @Override
    public CompoundTag serializeNBT(net.minecraft.core.HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (String s : unlockedCategories) {
            list.add(StringTag.valueOf(s));
        }
        tag.put("Unlocked", list);
        return tag;
    }

    @Override
    public void deserializeNBT(net.minecraft.core.HolderLookup.Provider provider, CompoundTag tag) {
        unlockedCategories.clear();
        if (tag.contains("Unlocked")) {
            ListTag list = tag.getList("Unlocked", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                unlockedCategories.add(list.getString(i));
            }
        }
    }
}