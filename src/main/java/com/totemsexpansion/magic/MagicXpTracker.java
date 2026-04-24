package com.totemsexpansion.magic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Awards Magic XP when a player kills a LivingEntity.
 *
 * <p>XP = {@code maxHealth × 0.5} (rounded down to the nearest integer). Hooks
 * {@link ServerLivingEntityEvents#AFTER_DEATH} and checks if the killer is a
 * {@link ServerPlayer}.</p>
 */
public final class MagicXpTracker {
    private MagicXpTracker() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(MagicXpTracker::onDeath);
    }

    private static void onDeath(LivingEntity victim, net.minecraft.world.damagesource.DamageSource source) {
        if (victim instanceof Player) return; // don't award XP for player deaths
        if (!(victim.level() instanceof ServerLevel sl)) return;
        if (!(source.getEntity() instanceof ServerPlayer killer)) return;
        long xp = (long) Math.max(1, Math.floor(victim.getMaxHealth() * 0.5));
        MagicPlayerData data = MagicPlayerData.of(sl.getServer());
        MagicPlayerData.Entry before = data.get(killer.getUUID());
        int beforeLevel = before.level();
        data.addXp(killer.getUUID(), xp);
        MagicPlayerData.Entry after = data.get(killer.getUUID());
        int afterLevel = after.level();
        // Push a sync packet so the HUD updates immediately.
        ServerPlayNetworking.send(killer, new MagicNetwork.SyncStatePayload(
                after.school().map(Enum::ordinal).orElse(-1),
                after.xp()));
        if (afterLevel > beforeLevel) {
            ServerPlayNetworking.send(killer, new MagicNetwork.LevelUpPayload(afterLevel));
        }
    }
}
