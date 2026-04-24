package com.totemsexpansion.magic;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Wires up server-side handling for Magic-related C2S packets plus the initial
 * sync when a player joins.
 */
public final class MagicServerReceivers {
    private MagicServerReceivers() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(MagicNetwork.PickSchoolPayload.TYPE,
                (payload, ctx) -> ctx.server().execute(() -> {
                    if (payload.ordinal() < 0 || payload.ordinal() >= MagicSchool.values().length) return;
                    MagicSchool school = MagicSchool.values()[payload.ordinal()];
                    MagicPlayerData data = MagicPlayerData.of(ctx.server());
                    MagicPlayerData.Entry cur = data.get(ctx.player().getUUID());
                    if (cur.school().isPresent()) return;
                    data.setSchool(ctx.player().getUUID(), school);
                    MagicPlayerData.Entry now = data.get(ctx.player().getUUID());
                    ServerPlayNetworking.send(ctx.player(), new MagicNetwork.SyncStatePayload(
                            now.school().map(Enum::ordinal).orElse(-1), now.xp()));
                }));

        ServerPlayNetworking.registerGlobalReceiver(MagicNetwork.CastAbilityPayload.TYPE,
                (payload, ctx) -> ctx.server().execute(() -> {
                    var player = ctx.player();
                    MagicPlayerData data = MagicPlayerData.of(ctx.server());
                    MagicPlayerData.Entry entry = data.get(player.getUUID());
                    if (entry.school().isEmpty()) {
                        player.sendOverlayMessage(Component.literal("✦ Выбери школу магии сначала")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                        return;
                    }
                    if (!Levels.canUseAbility(entry.level(), payload.slot())) {
                        int need = Levels.unlockLevel(payload.slot());
                        player.sendOverlayMessage(Component.literal("🔒 Нужен уровень " + need
                                        + " (сейчас " + entry.level() + ")")
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                    // Require having a magic totem in the inventory.
                    boolean hasTotem = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        if (player.getInventory().getItem(i).getItem() == MagicTotemItem.INSTANCE) {
                            hasTotem = true;
                            break;
                        }
                    }
                    if (!hasTotem) {
                        player.sendOverlayMessage(Component.literal("✦ Нет Magic Totem в инвентаре")
                                .withStyle(ChatFormatting.GRAY));
                        return;
                    }
                    int remaining = Abilities.remainingCooldown(player, payload.slot());
                    if (remaining > 0) {
                        float sec = remaining / 20.0f;
                        player.sendOverlayMessage(Component.literal(String.format("⏳ Откат %.1fs", sec))
                                .withStyle(ChatFormatting.YELLOW));
                        return;
                    }
                    Abilities.cast(player, entry.school().get(), payload.slot());
                }));

        // Initial sync when player joins.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            MagicPlayerData data = MagicPlayerData.of(server);
            MagicPlayerData.Entry entry = data.get(handler.getPlayer().getUUID());
            ServerPlayNetworking.send(handler.getPlayer(), new MagicNetwork.SyncStatePayload(
                    entry.school().map(Enum::ordinal).orElse(-1), entry.xp()));
        });
    }
}
