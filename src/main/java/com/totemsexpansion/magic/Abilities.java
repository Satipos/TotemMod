package com.totemsexpansion.magic;

import com.totemsexpansion.TotemsExpansionMod;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side execution for all 9 magic abilities (3 per school × 3 slots).
 *
 * <p>{@link #cast} is the public dispatcher: it takes the invoking player, their
 * school, and the slot (0–2). Cooldowns are enforced here via per-slot cooldown
 * groups so that each ability has its own timer.</p>
 */
public final class Abilities {
    private static final Identifier CD_SLOT_0 = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "ability_1");
    private static final Identifier CD_SLOT_1 = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "ability_2");
    private static final Identifier CD_SLOT_2 = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "ability_ult");

    private static final int[] COOLDOWN_TICKS = {
            40,   // slot 0 — 2 s
            120,  // slot 1 — 6 s
            600   // slot 2 — 30 s
    };

    private Abilities() {}

    // Simple in-memory cooldown tracker (per player UUID, per slot) — lost on server stop, which is fine.
    private static final java.util.Map<java.util.UUID, long[]> LAST_CAST_GAMETIME = new java.util.HashMap<>();

    public static Identifier cooldownGroup(int slot) {
        return switch (slot) { case 0 -> CD_SLOT_0; case 1 -> CD_SLOT_1; default -> CD_SLOT_2; };
    }

    public static int cooldownTicks(int slot) {
        return COOLDOWN_TICKS[Math.min(slot, 2)];
    }

    /** Remaining ticks until the given slot is usable again for this player. 0 means ready. */
    public static int remainingCooldown(ServerPlayer p, int slot) {
        long[] arr = LAST_CAST_GAMETIME.get(p.getUUID());
        if (arr == null) return 0;
        long elapsed = p.level().getGameTime() - arr[slot];
        int cd = cooldownTicks(slot);
        return (int) Math.max(0, cd - elapsed);
    }

    public static void cast(ServerPlayer player, MagicSchool school, int slot) {
        if (slot < 0 || slot > 2) return;
        Level rawLevel = player.level();
        if (!(rawLevel instanceof ServerLevel level)) return;

        if (remainingCooldown(player, slot) > 0) return;

        switch (school) {
            case AIR   -> castAir(player, level, slot);
            case EARTH -> castEarth(player, level, slot);
            case FIRE  -> castFire(player, level, slot);
        }

        long[] arr = LAST_CAST_GAMETIME.computeIfAbsent(player.getUUID(), u -> new long[3]);
        arr[slot] = level.getGameTime();
    }

    // ============================================================
    //   AIR
    // ============================================================

    private static void castAir(ServerPlayer p, ServerLevel level, int slot) {
        switch (slot) {
            case 0 -> airWindGust(p, level);
            case 1 -> airSkyStrike(p, level);
            case 2 -> airTempestEye(p, level);
        }
    }

    private static void airWindGust(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle().normalize();
        Vec3 origin = p.getEyePosition().add(look.scale(0.5));
        // 6-block cone forward.
        AABB box = new AABB(origin.subtract(6, 4, 6), origin.add(6, 4, 6));
        DamageSource src = level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == p) continue;
            Vec3 toEnt = e.position().subtract(p.position()).normalize();
            if (toEnt.dot(look) < 0.3) continue; // must be roughly in front
            if (e.distanceTo(p) > 7) continue;
            e.hurtServer(level, src, 5.0f);
            Vec3 push = look.scale(2.2).add(0, 0.5, 0);
            e.setDeltaMovement(push);
            e.hurtMarked = true;
        }
        // VFX — cone of cloud particles.
        for (int i = 0; i < 25; i++) {
            double t = i / 25.0;
            Vec3 at = origin.add(look.scale(t * 6));
            level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 8, 0.4, 0.4, 0.4, 0.08);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.5f, 1.1f);
    }

    private static void airSkyStrike(ServerPlayer p, ServerLevel level) {
        HitResult hit = p.pick(30.0, 1.0f, false);
        Vec3 target = hit.getLocation();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.setPos(target.x, target.y, target.z);
        bolt.setCause(p);
        level.addFreshEntity(bolt);
        // Extra: deal 10 magic damage to entities within 3 blocks of the impact that aren't the caster.
        AABB aoe = new AABB(target.subtract(3, 3, 3), target.add(3, 3, 3));
        DamageSource src = level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aoe, LivingEntity::isAlive)) {
            if (e == p) continue;
            e.hurtServer(level, src, 10.0f);
        }
        level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 1, target.z,
                40, 1.5, 2.0, 1.5, 0.1);
    }

    private static void airTempestEye(ServerPlayer p, ServerLevel level) {
        // 8-second storm around the player. One-shot dramatic event, damage pulses every second.
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 2));
        AABB box = new AABB(p.position().subtract(10, 5, 10), p.position().add(10, 5, 10));
        DamageSource src = level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == p) continue;
            e.hurtServer(level, src, 18.0f);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 2.5, 0));
            e.hurtMarked = true;
            // Chance-based lightning visual.
            if (level.getRandom().nextFloat() < 0.4f) {
                LightningBolt b = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (b != null) {
                    b.setVisualOnly(true);
                    b.setPos(e.getX(), e.getY(), e.getZ());
                    level.addFreshEntity(b);
                }
            }
        }
        // Spiraling wind VFX.
        for (int t = 0; t < 72; t++) {
            double a = Math.toRadians(t * 10);
            double r = 6.0;
            for (int y = 0; y < 8; y++) {
                double x = p.getX() + Math.cos(a + y * 0.2) * r;
                double z = p.getZ() + Math.sin(a + y * 0.2) * r;
                level.sendParticles(ParticleTypes.CLOUD, x, p.getY() + y, z, 1, 0, 0, 0, 0.02);
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 4.0f, 0.9f);
        level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 3.0f, 1.0f);
    }

    // ============================================================
    //   EARTH
    // ============================================================

    private static void castEarth(ServerPlayer p, ServerLevel level, int slot) {
        switch (slot) {
            case 0 -> earthRockBurst(p, level);
            case 1 -> earthStoneSpike(p, level);
            case 2 -> earthEarthquake(p, level);
        }
    }

    private static void earthRockBurst(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle().normalize();
        AABB box = new AABB(p.position().subtract(6, 3, 6), p.position().add(6, 3, 6));
        DamageSource src = level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == p) continue;
            Vec3 to = e.position().subtract(p.position()).normalize();
            if (to.dot(look) < 0.0) continue;
            if (e.distanceTo(p) > 6) continue;
            e.hurtServer(level, src, 7.0f);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
        }
        // Ground-level "rock" spray VFX in front.
        for (int i = 0; i < 40; i++) {
            double d = 1 + (i / 40.0) * 5.0;
            Vec3 at = p.position().add(look.scale(d)).add((level.getRandom().nextDouble() - 0.5) * 2,
                    level.getRandom().nextDouble() * 0.5, (level.getRandom().nextDouble() - 0.5) * 2);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                    at.x, at.y, at.z, 4, 0.3, 0.3, 0.3, 0.15);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS, 2.0f, 0.7f);
    }

    private static void earthStoneSpike(ServerPlayer p, ServerLevel level) {
        HitResult hit = p.pick(25.0, 1.0f, false);
        Vec3 target = hit.getLocation();
        AABB aoe = new AABB(target.subtract(2, 2, 2), target.add(2, 2, 2));
        DamageSource src = level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, aoe, LivingEntity::isAlive)) {
            if (e == p) continue;
            e.hurtServer(level, src, 14.0f);
            e.setDeltaMovement(e.getDeltaMovement().add(0, 1.2, 0));
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            e.hurtMarked = true;
        }
        // Spike VFX — vertical column of stone block particles.
        for (int y = 0; y < 4; y++) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState()),
                    target.x, target.y + y, target.z, 15, 0.6, 0.2, 0.6, 0.05);
        }
        level.playSound(null, new net.minecraft.core.BlockPos((int) target.x, (int) target.y, (int) target.z),
                SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 2.0f, 0.8f);
    }

    private static void earthEarthquake(ServerPlayer p, ServerLevel level) {
        AABB box = new AABB(p.position().subtract(20, 6, 20), p.position().add(20, 6, 20));
        DamageSource src = level.damageSources().magic();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == p) continue;
            e.hurtServer(level, src, 30.0f);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 2));
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.5, 0));
            e.hurtMarked = true;
        }
        // Ripple rings of stone particles.
        for (int ring = 0; ring < 6; ring++) {
            double r = 3 + ring * 3;
            for (int d = 0; d < 360; d += 6) {
                double a = Math.toRadians(d);
                double x = p.getX() + Math.cos(a) * r;
                double z = p.getZ() + Math.sin(a) * r;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                        x, p.getY() + 0.2, z, 2, 0.1, 0.2, 0.1, 0.1);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, p.getY() + 1, z, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 4.0f, 0.6f);
        level.playSound(null, p.blockPosition(), SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                SoundSource.PLAYERS, 3.0f, 0.8f);
    }

    // ============================================================
    //   FIRE
    // ============================================================

    private static void castFire(ServerPlayer p, ServerLevel level, int slot) {
        switch (slot) {
            case 0 -> fireFireball(p, level);
            case 1 -> fireFlameBreath(p, level);
            case 2 -> fireMeteor(p, level);
        }
    }

    private static void fireFireball(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle().normalize();
        SmallFireball fb = new SmallFireball(level, p, look.scale(1.2));
        fb.setPos(p.getX() + look.x * 1.2, p.getEyeY() + look.y * 1.2 - 0.2, p.getZ() + look.z * 1.2);
        level.addFreshEntity(fb);
        level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 1.5f, 1.0f);
    }

    private static void fireFlameBreath(ServerPlayer p, ServerLevel level) {
        Vec3 look = p.getLookAngle().normalize();
        DamageSource src = level.damageSources().onFire();
        // 6-block cone in front of player.
        for (int step = 1; step <= 12; step++) {
            double d = step * 0.5;
            Vec3 at = p.getEyePosition().add(look.scale(d));
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, 0.4, 0.4, 0.4, 0.05);
            level.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y, at.z, 4, 0.3, 0.3, 0.3, 0.05);
        }
        AABB box = new AABB(p.position().subtract(7, 4, 7), p.position().add(7, 4, 7));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == p) continue;
            Vec3 to = e.position().subtract(p.position()).normalize();
            if (to.dot(look) < 0.5) continue;
            if (e.distanceTo(p) > 7) continue;
            e.hurtServer(level, src, 12.0f);
            e.igniteForSeconds(6);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 2.5f, 0.9f);
        level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_BURN,
                SoundSource.PLAYERS, 2.0f, 0.7f);
    }

    private static void fireMeteor(ServerPlayer p, ServerLevel level) {
        HitResult hit = p.pick(40.0, 1.0f, false);
        Vec3 impact = hit.getLocation();
        // Telegraph burst at target site.
        level.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 0.5, impact.z, 40, 3.0, 0.5, 3.0, 0.1);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y + 0.5, impact.z, 1, 0, 0, 0, 0);
        // Trail streaking down from the sky.
        for (int y = 0; y < 30; y += 2) {
            double yy = impact.y + y;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, impact.x, yy, impact.z, 4, 1.0, 0.5, 1.0, 0.05);
            level.sendParticles(ParticleTypes.FLAME, impact.x, yy, impact.z, 6, 1.0, 0.5, 1.0, 0.1);
        }
        // Impact damage + ignite all in 8 blocks.
        AABB box = new AABB(impact.subtract(8, 5, 8), impact.add(8, 5, 8));
        DamageSource src = level.damageSources().onFire();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == p) continue;
            double d = e.distanceToSqr(impact);
            float dmg = (float) Math.max(5.0, 40.0 - Math.sqrt(d) * 3.5);
            e.hurtServer(level, src, dmg);
            e.igniteForSeconds(10);
            Vec3 push = e.position().subtract(impact).normalize().scale(1.5).add(0, 1.0, 0);
            e.setDeltaMovement(push);
            e.hurtMarked = true;
        }
        // Cosmetic vanilla explosion (no block damage).
        level.explode(null, impact.x, impact.y, impact.z, 4.0f, Level.ExplosionInteraction.NONE);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 4.0f, 0.8f);
        level.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 3.0f, 1.4f);
    }
}
