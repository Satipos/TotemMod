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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

/**
 * Tornado Totem — when lethal damage hits the carrier:
 * <ul>
 *   <li>Consumes the totem stack (like Totem of Undying)</li>
 *   <li>Restores the player to 1.0 HP, clears fire and negative effects</li>
 *   <li>Launches every non-player living entity within 10 blocks straight up ({@code +3.0} Y velocity)</li>
 *   <li>Grants the player Slow Falling for 10 s so they can safely descend</li>
 * </ul>
 */
public final class TornadoTotemItem extends Item {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "tornado_totem");
    public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);
    public static TornadoTotemItem INSTANCE;

    private TornadoTotemItem(Properties props) {
        super(props);
    }

    public static void register() {
        Properties p = new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).setId(ITEM_KEY);
        INSTANCE = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, new TornadoTotemItem(p));
    }

    public void onLethalSave(ServerLevel level, Player player, ItemStack stack) {
        // 1. Restore the player.
        player.setHealth(1.0f);
        player.removeAllEffects();
        player.setRemainingFireTicks(0);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0)); // 10s
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));    // 5s
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));  // 5s

        // 2. Consume one totem.
        stack.shrink(1);

        // 3. Launch nearby hostile entities skyward.
        AABB box = new AABB(player.getX() - 10, player.getY() - 3, player.getZ() - 10,
                player.getX() + 10, player.getY() + 3, player.getZ() + 10);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player)) {
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            double dist = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
            // Gentle outward push so the mob doesn't land right back on the player.
            double horizontal = 0.25 / dist;
            e.setDeltaMovement(dx * horizontal, 3.0, dz * horizontal);
            e.hurtMarked = true;
            // Don't add slow falling — we want gravity to crash them back down.
        }

        // 4. VFX / SFX.
        for (int i = 0; i < 360; i += 6) {
            double a = Math.toRadians(i);
            for (double r = 0.5; r < 6.0; r += 0.5) {
                double x = player.getX() + Math.cos(a) * r;
                double z = player.getZ() + Math.sin(a) * r;
                double y = player.getY() + r * 0.5;
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                player.getX(), player.getY() + 1.2, player.getZ(),
                8, 0.5, 0.3, 0.5, 0.0);
        level.sendParticles(ParticleTypes.TRIAL_OMEN,
                player.getX(), player.getY() + 1.2, player.getZ(),
                30, 1.5, 2.0, 1.5, 0.0);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.5f, 0.9f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
    }
}
