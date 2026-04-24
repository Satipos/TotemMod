package com.totemsexpansion.magic;

import com.totemsexpansion.TotemsExpansionMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Magic Totem — right-click to either pick a magic school (one-time, permanent)
 * or to open the spellbook UI showing XP / level / abilities.
 *
 * <p>All persistent state lives in {@link MagicPlayerData} keyed by player UUID,
 * so losing / dropping the totem doesn't wipe the player's progress.</p>
 */
public final class MagicTotemItem extends Item {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "magic_totem");
    public static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, ID);
    public static MagicTotemItem INSTANCE;

    private MagicTotemItem(Properties p) {
        super(p);
    }

    public static void register() {
        Properties p = new Properties().stacksTo(1).rarity(Rarity.EPIC).setId(ITEM_KEY);
        INSTANCE = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY, new MagicTotemItem(p));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        MagicPlayerData data = MagicPlayerData.of(sp.level().getServer());
        MagicPlayerData.Entry entry = data.get(sp.getUUID());
        if (entry.school().isEmpty()) {
            ServerPlayNetworking.send(sp, new MagicNetwork.OpenSchoolScreenPayload());
        } else {
            ServerPlayNetworking.send(sp, new MagicNetwork.SyncStatePayload(
                    entry.school().get().ordinal(), entry.xp()));
            ServerPlayNetworking.send(sp, new MagicNetwork.OpenSpellbookPayload());
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
