package com.totemsexpansion.magic;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Brigadier commands for the Magic system.
 *
 * <ul>
 *   <li>{@code /xpgive <amount>} — grants magic XP to the caller.</li>
 *   <li>{@code /xpgive <amount> <player>} — grants magic XP to a target (op only).</li>
 * </ul>
 */
public final class MagicCommands {
    private MagicCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, selection) -> {
            dispatcher.register(Commands.literal("xpgive")
                    .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                            .executes(ctx -> {
                                long amount = LongArgumentType.getLong(ctx, "amount");
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                return give(player, amount, ctx.getSource());
                            })
                            .then(Commands.argument("target", net.minecraft.commands.arguments.EntityArgument.player())
                                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                    .executes(ctx -> {
                                        long amount = LongArgumentType.getLong(ctx, "amount");
                                        ServerPlayer target = net.minecraft.commands.arguments.EntityArgument
                                                .getPlayer(ctx, "target");
                                        return give(target, amount, ctx.getSource());
                                    }))));
        });
    }

    private static int give(ServerPlayer player, long amount, net.minecraft.commands.CommandSourceStack src) {
        MagicPlayerData data = MagicPlayerData.of(player.level().getServer());
        MagicPlayerData.Entry before = data.get(player.getUUID());
        int beforeLevel = before.level();
        data.addXp(player.getUUID(), amount);
        MagicPlayerData.Entry after = data.get(player.getUUID());

        // Push updated state to the client so HUD/spellbook show the new XP.
        ServerPlayNetworking.send(player, new MagicNetwork.SyncStatePayload(
                after.school().map(Enum::ordinal).orElse(-1), after.xp()));
        if (after.level() > beforeLevel) {
            ServerPlayNetworking.send(player, new MagicNetwork.LevelUpPayload(after.level()));
        }

        src.sendSuccess(() -> Component.literal("+" + amount + " magic XP → lvl " + after.level()
                + " (" + after.xp() + " total)").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }
}
