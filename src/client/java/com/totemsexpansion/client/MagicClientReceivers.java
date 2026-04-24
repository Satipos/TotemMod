package com.totemsexpansion.client;

import com.totemsexpansion.magic.MagicNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

/**
 * Registers all S2C packet handlers used by the Magic system.
 */
public final class MagicClientReceivers {
    private MagicClientReceivers() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(MagicNetwork.OpenSchoolScreenPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> safe(
                        () -> Minecraft.getInstance().setScreen(new SchoolSelectScreen()))));

        ClientPlayNetworking.registerGlobalReceiver(MagicNetwork.OpenSpellbookPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> safe(
                        () -> Minecraft.getInstance().setScreen(new SpellbookScreen()))));

        ClientPlayNetworking.registerGlobalReceiver(MagicNetwork.SyncStatePayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> safe(
                        () -> ClientMagicState.update(payload.school(), payload.xp()))));

        ClientPlayNetworking.registerGlobalReceiver(MagicNetwork.LevelUpPayload.TYPE,
                (payload, ctx) -> ctx.client().execute(() -> safe(() -> {
                    ClientMagicState.onLevelUp(payload.newLevel(), System.currentTimeMillis());
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null && mc.player != null) {
                        mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS,
                                1.5f, 1.0f, false);
                    }
                })));
    }

    private static void safe(Runnable r) {
        try { r.run(); } catch (Throwable t) {
            com.totemsexpansion.TotemsExpansionMod.LOGGER.error("Magic client packet handler error", t);
        }
    }
}
