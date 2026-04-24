package com.totemsexpansion.item;

import com.totemsexpansion.TotemsExpansionMod;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Cripper Totem — when lethal damage hits the carrier:
 * <ul>
 *   <li>Consumes the totem stack</li>
 *   <li>Saves the player to 1 HP and gives them 6 s Resistance V so the
 *       follow-up blast cannot harm them</li>
 *   <li>Triggers a manual radial explosion (no terrain damage, but big AoE
 *       20 damage) on all nearby LivingEntities</li>
 *   <li>Spawns a vanilla {@code Level.explode} for the camera shake / sound,
 *       configured to NOT damage blocks or the player</li>
 * </ul>
 */
public final class CripperTotemItem extends Item {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "cripper_totem");
    public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);
    public static CripperTotemItem INSTANCE;

    private static final float DAMAGE = 20.0f;
    private static final double RADIUS = 7.0;

    private CripperTotemItem(Properties p) {
        super(p);
    }

    public static void register() {
        Properties p = new Properties().stacksTo(1).rarity(Rarity.RARE).setId(ITEM_KEY);
        INSTANCE = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, new CripperTotemItem(p));
    }

    public void onLethalSave(ServerLevel level, Player player, ItemStack stack) {
        // 1. Save the player (include a brief invulnerability so the self-triggered blast
        //    can't knock them out again).
        player.setHealth(1.0f);
        player.removeAllEffects();
        player.setRemainingFireTicks(0);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 4));    // 6s Resistance V
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 2));     // 10s Absorption III
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));

        // 2. Consume the totem.
        stack.shrink(1);

        // 3. Custom AoE damage — skips the player.
        AABB box = new AABB(player.getX() - RADIUS, player.getY() - 3, player.getZ() - RADIUS,
                player.getX() + RADIUS, player.getY() + 3, player.getZ() + RADIUS);
        DamageSource src = level.damageSources().explosion(player, player);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            // Falloff so the edge does less damage than the core.
            double d = Math.max(1.0, e.distanceTo(player));
            float dmg = (float) Math.max(2.0, DAMAGE * (1.0 - d / (RADIUS * 1.5)));
            e.hurtServer(level, src, dmg);
            // Knockback outward.
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            double dist = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            double kb = 2.2 / dist;
            e.push(dx * kb, 0.9, dz * kb);
            e.hurtMarked = true;
        }

        // 4. Cosmetic vanilla explosion (no block damage, no player damage) for VFX + sound.
        level.explode(null, player.getX(), player.getY() + 0.5, player.getZ(),
                3.0f, false, Level.ExplosionInteraction.NONE);

        // 5. Extra VFX for drama.
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 0.8, player.getZ(),
                3, 1.2, 0.8, 1.2, 0.0);
        level.sendParticles(ParticleTypes.SONIC_BOOM,
                player.getX(), player.getY() + 1.2, player.getZ(),
                2, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 1.2, player.getZ(),
                120, 4.0, 3.0, 4.0, 0.1);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.2, player.getZ(),
                40, 2.0, 1.5, 2.0, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.8f);
        // note: GENERIC_EXPLODE is a Holder.Reference so we call .value()
        level.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 1.4f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
