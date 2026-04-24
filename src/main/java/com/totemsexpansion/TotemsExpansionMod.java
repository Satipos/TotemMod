package com.totemsexpansion;

import com.totemsexpansion.item.CripperTotemItem;
import com.totemsexpansion.item.FireTotemItem;
import com.totemsexpansion.item.ModCreativeTab;
import com.totemsexpansion.item.TornadoTotemItem;
import com.totemsexpansion.magic.MagicCommands;
import com.totemsexpansion.magic.MagicNetwork;
import com.totemsexpansion.magic.MagicServerReceivers;
import com.totemsexpansion.magic.MagicTotemItem;
import com.totemsexpansion.magic.MagicXpTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint. Registers the 3 totem items, the custom creative tab, and the
 * {@link ServerLivingEntityEvents#ALLOW_DEATH} hook that implements the
 * "save the player and trigger their totem's on-consume effect" behavior.
 */
public class TotemsExpansionMod implements ModInitializer {
    public static final String MOD_ID = "totemsexpansion";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[{}] Initializing Totems Expansion", MOD_ID);

        // IMPORTANT: register items before the creative tab (the tab's
        // displayItems generator references these instances).
        TornadoTotemItem.register();
        FireTotemItem.register();
        CripperTotemItem.register();
        MagicTotemItem.register();
        ModCreativeTab.register();

        MagicNetwork.registerCommon();
        MagicServerReceivers.register();
        MagicXpTracker.register();
        MagicCommands.register();

        // Hook: intercept fatal damage on a player. If they're holding one of our
        // totems in either hand, save them and trigger the totem's effect.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof Player player)) return true;
            if (!(player.level() instanceof ServerLevel level)) return true;

            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.isEmpty()) continue;

                if (held.getItem() instanceof TornadoTotemItem tornado) {
                    tornado.onLethalSave(level, player, held);
                    return false;
                }
                if (held.getItem() instanceof FireTotemItem fire) {
                    fire.onLethalSave(level, player, held);
                    return false;
                }
                if (held.getItem() instanceof CripperTotemItem cripper) {
                    cripper.onLethalSave(level, player, held);
                    return false;
                }
            }
            return true;
        });
    }
}
