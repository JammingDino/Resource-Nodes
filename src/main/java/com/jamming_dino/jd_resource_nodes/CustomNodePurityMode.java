package com.jamming_dino.jd_resource_nodes;

import java.util.List;

public enum CustomNodePurityMode {
    IMPURE_ONLY("impure", List.of(ResourceNodeTier.IMPURE), "Impure Only"),
    NORMAL_ONLY("normal", List.of(ResourceNodeTier.NORMAL), "Normal Only"),
    PURE_ONLY("pure", List.of(ResourceNodeTier.PURE), "Pure Only"),
    ALL("all", List.of(ResourceNodeTier.IMPURE, ResourceNodeTier.NORMAL, ResourceNodeTier.PURE), "All Tiers");

    private final String id;
    private final List<ResourceNodeTier> tiers;
    private final String displayName;

    CustomNodePurityMode(String id, List<ResourceNodeTier> tiers, String displayName) {
        this.id = id;
        this.tiers = tiers;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public List<ResourceNodeTier> getTiers() {
        return tiers;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CustomNodePurityMode fromId(String id) {
        if (id == null) {
            return ALL;
        }
        for (CustomNodePurityMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id.trim())) {
                return mode;
            }
        }
        return ALL;
    }
}

