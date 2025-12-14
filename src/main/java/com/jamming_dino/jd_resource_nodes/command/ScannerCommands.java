package com.jamming_dino.jd_resource_nodes.command;

import com.jamming_dino.jd_resource_nodes.ResourceNodeData;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import com.jamming_dino.jd_resource_nodes.network.SyncScannerUnlocksPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ScannerCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("scanner")
                .requires(source -> source.hasPermission(2)) // Operator Level 2
                .then(Commands.literal("unlock")
                        .then(Commands.literal("all")
                                .executes(ScannerCommands::unlockAll)
                        )
                        .then(Commands.argument("resource", StringArgumentType.word())
                                .suggests(ScannerCommands::suggestResources)
                                .executes(ScannerCommands::unlockResource)
                        )
                )
                .then(Commands.literal("lock")
                        .then(Commands.literal("all")
                                .executes(ScannerCommands::lockAll)
                        )
                        .then(Commands.argument("resource", StringArgumentType.word())
                                .suggests(ScannerCommands::suggestResources)
                                .executes(ScannerCommands::lockResource)
                        )
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestResources(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                ResourceNodeData.getAllCategories().stream().map(ResourceNodeData::getCategory),
                builder
        );
    }

    private static int unlockResource(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String resource = StringArgumentType.getString(context, "resource");

        // Validate resource exists
        if (ResourceNodeData.getByCategory(resource) == null) {
            context.getSource().sendFailure(Component.literal("Unknown resource category: " + resource));
            return 0;
        }

        ScannerUnlockData data = player.getData(ResourceNodes.SCANNER_DATA);
        if (data.unlock(resource)) {
            syncData(player, data);
            context.getSource().sendSuccess(() -> Component.literal("Unlocked scanning for: " + resource), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Already unlocked: " + resource));
            return 0;
        }
    }

    private static int lockResource(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String resource = StringArgumentType.getString(context, "resource");

        ScannerUnlockData data = player.getData(ResourceNodes.SCANNER_DATA);
        if (data.lock(resource)) {
            syncData(player, data);
            context.getSource().sendSuccess(() -> Component.literal("Locked scanning for: " + resource), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Was not unlocked: " + resource));
            return 0;
        }
    }

    private static int unlockAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ScannerUnlockData data = player.getData(ResourceNodes.SCANNER_DATA);

        data.unlockAll(ResourceNodeData.getAllCategories().stream()
                .map(ResourceNodeData::getCategory)
                .collect(Collectors.toSet()));

        syncData(player, data);
        context.getSource().sendSuccess(() -> Component.literal("Unlocked all resource nodes."), true);
        return 1;
    }

    private static int lockAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ScannerUnlockData data = player.getData(ResourceNodes.SCANNER_DATA);

        data.lockAll();

        syncData(player, data);
        context.getSource().sendSuccess(() -> Component.literal("Locked all resource nodes."), true);
        return 1;
    }

    private static void syncData(ServerPlayer player, ScannerUnlockData data) {
        PacketDistributor.sendToPlayer(player, new SyncScannerUnlocksPacket(data.getUnlockedCategories()));
    }
}