package jammingdino.jd_resource_nodes;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class ResourceNodesDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // 1. Models (Visuals)
        pack.addProvider(ResourceNodesModelProvider::new);

        // 2. Tags (Mining Levels)
        pack.addProvider(ResourceNodesBlockTagProvider::new);

        // 3. Language (Names) - NEW
        pack.addProvider(ResourceNodesLangProvider::new);
    }
}