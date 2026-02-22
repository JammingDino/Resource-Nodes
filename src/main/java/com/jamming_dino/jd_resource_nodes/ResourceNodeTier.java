package com.jamming_dino.jd_resource_nodes;

import net.minecraft.util.StringRepresentable;

public enum ResourceNodeTier implements StringRepresentable {
    IMPURE("impure"),
    NORMAL("normal"),
    PURE("pure");

    private final String name;

    ResourceNodeTier(String name) {
        this.name = name;
    }

    public int getDropCount() {
        switch (this) {
            case IMPURE: return 1;
            case NORMAL: return 2;
            case PURE:   return 3;
            default:     return 1;
        }
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}