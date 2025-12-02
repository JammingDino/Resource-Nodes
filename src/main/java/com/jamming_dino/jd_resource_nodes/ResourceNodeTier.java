package com.jamming_dino.jd_resource_nodes;

import net.minecraft.util.StringRepresentable;

// 'StringIdentifiable' became 'StringRepresentable' in newer mappings
public enum ResourceNodeTier implements StringRepresentable {
    IMPURE("impure"),
    NORMAL("normal"),
    PURE("pure");

    private final String name;

    ResourceNodeTier(String name) {
        this.name = name;
    }

    public int getRegenerateTicks() {
        switch (this) {
            case IMPURE: return ResourceNodesConfig.getImpureTicks();
            case NORMAL: return ResourceNodesConfig.getNormalTicks();
            case PURE:   return ResourceNodesConfig.getPureTicks();
            default:     return 600;
        }
    }

    @Override
    public String getSerializedName() { // 'asString' is now 'getSerializedName'
        return name;
    }
}