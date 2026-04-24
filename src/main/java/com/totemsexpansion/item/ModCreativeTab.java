package com.totemsexpansion.item;

import com.totemsexpansion.TotemsExpansionMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Custom creative tab "Totems Expansion" exposing all 3 totems + their crafting ingredients.
 *
 * <p>The tab uses the Tornado Totem as its icon. Display items are ordered so the
 * most defensive (Tornado → Fire → Cripper) appear first, followed by a tiny
 * crafting helper section that shows the vanilla Totem of Undying and the three
 * recipe ingredients, so the player can tell at a glance what each totem is made from.
 */
public final class ModCreativeTab {
    public static final Identifier TAB_ID =
            Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "main");
    public static final ResourceKey<CreativeModeTab> TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, TAB_ID);

    private ModCreativeTab() {}

    public static void register() {
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup." + TotemsExpansionMod.MOD_ID + ".main")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .icon(() -> new ItemStack(TornadoTotemItem.INSTANCE))
                .displayItems((params, output) -> {
                    // Three custom totems (ordered from defensive → offensive).
                    output.accept(new ItemStack(TornadoTotemItem.INSTANCE));
                    output.accept(new ItemStack(FireTotemItem.INSTANCE));
                    output.accept(new ItemStack(CripperTotemItem.INSTANCE));
                    // A little crafting reference so players know the ingredients at a glance.
                    output.accept(new ItemStack(Items.TOTEM_OF_UNDYING));
                    output.accept(new ItemStack(Items.WIND_CHARGE));
                    output.accept(new ItemStack(Items.BLAZE_POWDER));
                    output.accept(new ItemStack(Items.TNT));
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, tab);
    }
}
