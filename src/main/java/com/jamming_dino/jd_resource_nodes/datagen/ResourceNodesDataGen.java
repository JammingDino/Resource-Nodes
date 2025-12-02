package com.jamming_dino.jd_resource_nodes.datagen;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

public class ResourceNodesDataGen {

    // We removed @EventBusSubscriber. We will register this manually in the main class.
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Client: Models & Lang
        generator.addProvider(event.includeClient(), new ResourceNodesLangProvider(packOutput, "en_us"));
        generator.addProvider(event.includeClient(), new ResourceNodesModelProvider(packOutput, existingFileHelper));

        // Server: Tags
        generator.addProvider(event.includeServer(), new ResourceNodesBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));
    }
}