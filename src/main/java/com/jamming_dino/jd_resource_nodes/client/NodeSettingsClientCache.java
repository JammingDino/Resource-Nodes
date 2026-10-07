package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NodeSettingsClientCache {
    private static Set<String> disabledGlobal = new HashSet<>();
    private static Set<String> disabledWorld = new HashSet<>();
    private static Set<String> customNodeIds = new HashSet<>();

    private NodeSettingsClientCache() {
    }

    public static void update(Set<String> globalIds, Set<String> worldIds) {
        disabledGlobal = new HashSet<>(globalIds);
        disabledWorld = new HashSet<>(worldIds);
    }

    public static void updateScannerUnlocks(Set<String> unlocks) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            ScannerUnlockData data = ScannerUnlockData.get(player);
            data.setUnlockedCategories(unlocks);
            ScannerUnlockData.save(player, data);
        }
    }

    public static void updateCustomNodeIds(Set<String> ids) {
        customNodeIds = new HashSet<>(ids);
    }

    public static boolean isEnabled(String blockId, boolean worldMode) {
        if (disabledGlobal.contains(blockId)) {
            return false;
        }
        if (worldMode) {
            return !disabledWorld.contains(blockId);
        }
        return true;
    }

    public static Set<String> getDisabledGlobal() {
        return Collections.unmodifiableSet(disabledGlobal);
    }

    public static Set<String> getDisabledWorld() {
        return Collections.unmodifiableSet(disabledWorld);
    }

    public static Set<String> getCustomNodeIds() {
        return Collections.unmodifiableSet(customNodeIds);
    }
}


