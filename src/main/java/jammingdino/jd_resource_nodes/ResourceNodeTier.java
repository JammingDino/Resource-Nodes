package jammingdino.jd_resource_nodes;

import net.minecraft.util.StringIdentifiable;

public enum ResourceNodeTier implements StringIdentifiable {
    // We no longer pass the hardcoded ticks to the constructor
    IMPURE("impure"),
    NORMAL("normal"),
    PURE("pure");

    private final String name;

    ResourceNodeTier(String name) {
        this.name = name;
    }

    // This method now checks the Config class dynamically
    public int getRegenerateTicks() {
        switch (this) {
            case IMPURE: return ResourceNodesConfig.getImpureTicks();
            case NORMAL: return ResourceNodesConfig.getNormalTicks();
            case PURE:   return ResourceNodesConfig.getPureTicks();
            default:     return 600;
        }
    }

    @Override
    public String asString() {
        return name;
    }
}