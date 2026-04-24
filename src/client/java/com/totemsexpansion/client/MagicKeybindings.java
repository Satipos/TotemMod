package com.totemsexpansion.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.totemsexpansion.TotemsExpansionMod;
import com.totemsexpansion.magic.MagicNetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Three client-side key bindings (Z / X / C) that each fire a C2S
 * {@link MagicNetwork.CastAbilityPayload}.
 *
 * <p>They all live under a new {@link KeyMapping.Category} so the player can
 * re-bind them from the vanilla controls screen.</p>
 */
public final class MagicKeybindings {
    private MagicKeybindings() {}

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "magic"));

    public static KeyMapping ability1;
    public static KeyMapping ability2;
    public static KeyMapping ultimate;

    public static void register() {
        ability1 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.totemsexpansion.ability_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY));
        ability2 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.totemsexpansion.ability_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY));
        ultimate = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.totemsexpansion.ultimate",  InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (ability1.consumeClick()) ClientPlayNetworking.send(new MagicNetwork.CastAbilityPayload(0));
            while (ability2.consumeClick()) ClientPlayNetworking.send(new MagicNetwork.CastAbilityPayload(1));
            while (ultimate.consumeClick()) ClientPlayNetworking.send(new MagicNetwork.CastAbilityPayload(2));
        });
    }
}
