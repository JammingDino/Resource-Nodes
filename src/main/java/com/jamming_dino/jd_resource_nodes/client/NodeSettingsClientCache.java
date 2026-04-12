package com.jamming_dino.jd_resource_nodes.client;

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


