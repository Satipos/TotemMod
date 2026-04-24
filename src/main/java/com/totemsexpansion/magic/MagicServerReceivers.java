package com.totemsexpansion.magic;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

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
                    MagicPlayerData data = MagicPlayerData.of(ctx.server());
                    MagicPlayerData.Entry entry = data.get(ctx.player().getUUID());
                    if (entry.school().isEmpty()) return;
                    if (!Levels.canUseAbility(entry.level(), payload.slot())) return;
                    // Require having a magic totem in the inventory.
                    boolean hasTotem = false;
                    for (int i = 0; i < ctx.player().getInventory().getContainerSize(); i++) {
                        if (ctx.player().getInventory().getItem(i).getItem() == MagicTotemItem.INSTANCE) {
                            hasTotem = true;
                            break;
                        }
                    }
                    if (!hasTotem) return;
                    Abilities.cast(ctx.player(), entry.school().get(), payload.slot());
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
