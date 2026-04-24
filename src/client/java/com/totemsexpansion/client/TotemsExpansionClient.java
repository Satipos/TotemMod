package com.totemsexpansion.client;

import com.totemsexpansion.TotemsExpansionMod;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint — wires up the Magic Totem UI, HUD, keybindings, and S2C packet handlers.
 */
public class TotemsExpansionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TotemsExpansionMod.LOGGER.info("[{}] Client side loaded", TotemsExpansionMod.MOD_ID);
        MagicClientReceivers.register();
        MagicKeybindings.register();
        MagicHud.register();
    }
}
