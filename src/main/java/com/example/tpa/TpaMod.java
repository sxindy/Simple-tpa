package com.example.tpa;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TpaMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simpletpa");

    @Override
    public void onInitialize() {
        TpaConfig.load();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tpa")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> TpaManager.start(
                        ctx.getSource().getPlayerOrThrow(),
                        EntityArgumentType.getPlayer(ctx, "player"), false))));

            dispatcher.register(CommandManager.literal("tpahere")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> TpaManager.start(
                        ctx.getSource().getPlayerOrThrow(),
                        EntityArgumentType.getPlayer(ctx, "player"), true))));

            dispatcher.register(CommandManager.literal("tpaccept")
                .executes(ctx -> TpaManager.accept(ctx.getSource().getPlayerOrThrow(), null))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> TpaManager.accept(
                        ctx.getSource().getPlayerOrThrow(),
                        EntityArgumentType.getPlayer(ctx, "player").getUuid()))));

            dispatcher.register(CommandManager.literal("tpdeny")
                .executes(ctx -> TpaManager.deny(ctx.getSource().getPlayerOrThrow(), null))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> TpaManager.deny(
                        ctx.getSource().getPlayerOrThrow(),
                        EntityArgumentType.getPlayer(ctx, "player").getUuid()))));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            TpaManager.clear(handler.player.getUuid()));
    }
}
