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
import net.minecraft.world.phys.AABB;

/**
 * Fire Totem — when lethal damage hits the carrier:
 * <ul>
 *   <li>Consumes the totem stack</li>
 *   <li>Saves the player (1 HP + 15 s Fire Resistance)</li>
 *   <li>Deals 5.0 damage (2.5 hearts) to all nearby hostile LivingEntities</li>
 *   <li>Sets them on fire for 3 seconds (vanilla 1 HP/s fire tick)</li>
 * </ul>
 */
public final class FireTotemItem extends Item {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "fire_totem");
    public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);
    public static FireTotemItem INSTANCE;

    private FireTotemItem(Properties p) {
        super(p);
    }

    public static void register() {
        Properties p = new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).setId(ITEM_KEY);
        INSTANCE = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, new FireTotemItem(p));
    }

    public void onLethalSave(ServerLevel level, Player player, ItemStack stack) {
        // 1. Restore player.
        player.setHealth(1.0f);
        player.removeAllEffects();
        player.setRemainingFireTicks(0);
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0)); // 15s
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));

        // 2. Consume one totem.
        stack.shrink(1);

        // 3. Damage & ignite nearby hostiles (8-block radius).
        AABB box = new AABB(player.getX() - 8, player.getY() - 3, player.getZ() - 8,
                player.getX() + 8, player.getY() + 3, player.getZ() + 8);
        DamageSource src = level.damageSources().onFire();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            e.hurtServer(level, src, 5.0f);
            e.igniteForSeconds(3);
        }

        // 4. VFX / SFX — expanding ring of flame + ember burst.
        for (int i = 0; i < 360; i += 8) {
            double a = Math.toRadians(i);
            for (double r = 1.0; r < 8.0; r += 1.0) {
                double x = player.getX() + Math.cos(a) * r;
                double z = player.getZ() + Math.sin(a) * r;
                double y = player.getY() + 0.4;
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.1, 0.2, 0.1, 0.02);
                if (r % 2 == 0) {
                    level.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1.2, player.getZ(),
                4, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                player.getX(), player.getY() + 1.2, player.getZ(),
                40, 2.5, 2.0, 2.5, 0.02);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1.2, player.getZ(),
                80, 3.0, 1.5, 3.0, 0.08);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0f, 0.9f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
