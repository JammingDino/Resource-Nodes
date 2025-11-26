package jammingdino.jd_resource_nodes;

import jammingdino.jd_resource_nodes.block.ResourceNodeBlock;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ResourceNodesLangProvider extends FabricLanguageProvider {

    public ResourceNodesLangProvider(FabricDataOutput dataOutput) {
        super(dataOutput, "en_us");
    }

    @Override
    public void generateTranslations(TranslationBuilder builder) {
        builder.add("itemGroup.jd_resource_nodes.general", "Resource Nodes");

        for (ResourceNodeBlock node : ResourceNodes.REGISTERED_NODES) {
            Identifier id = Registries.BLOCK.getId(node);
            String path = id.getPath(); // e.g. "node_iron_pure"
            builder.add(node, generateDisplayName(path));
        }
    }

    private String generateDisplayName(String path) {
        // 1. Replace underscores with spaces
        String spaced = path.replace('_', ' ');

        // 2. Capitalize words manually
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : spaced.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}