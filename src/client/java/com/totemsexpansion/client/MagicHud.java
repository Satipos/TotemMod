package com.totemsexpansion.client;

import com.totemsexpansion.TotemsExpansionMod;
import com.totemsexpansion.magic.Levels;
import com.totemsexpansion.magic.MagicSchool;
import com.totemsexpansion.magic.MagicTotemItem;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Persistent HUD shown when the player has a Magic Totem in their inventory.
 *
 * <p>v1.6.0: always shows level + XP bar even when a school hasn't been picked yet,
 * so the player can visually confirm XP is being awarded. Ability pips only render
 * once a school is chosen.</p>
 */
public final class MagicHud {
    private MagicHud() {}

    public static final Identifier ID = Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "magic_hud");

    public static void register() {
        HudElementRegistry.addLast(ID, (HudElement) MagicHud::render);
    }

    private static void render(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;
        if (!hasTotemAnywhere(mc.player)) return;

        Optional<MagicSchool> schoolOpt = ClientMagicState.school();
        int accent = schoolOpt.map(s -> s.colorArgb).orElse(0xFFB24CFF);

        int w = 196;
        int h = schoolOpt.isPresent() ? 82 : 46;
        int x = g.guiWidth() - w - 10;
        int y = 10;

        // Outer glow — subtle pulse.
        long now = System.currentTimeMillis();
        int pulseA = 0x20 + (int)(Math.sin(now / 400.0) * 0x10);
        for (int k = 6; k >= 2; k--) {
            int a = Math.max(0, pulseA - (6 - k) * 0x06);
            g.fill(x - k, y - k, x + w + k, y + h + k, (a << 24) | (accent & 0xFFFFFF));
        }

        // Main panel.
        UiFx.fancyPanel(g, x, y, x + w, y + h,
                0xEE18102A, 0xDD0A0818, accent | 0xFF000000, accent | 0xFF000000);

        // Header row with insignia + text.
        int headerY = y + 6;
        if (schoolOpt.isPresent()) {
            MagicSchool school = schoolOpt.get();
            // Miniature insignia.
            UiFx.fillCircle(g, x + 14, headerY + 6, 8, UiFx.darken(accent, 0.4f) | 0xFF000000);
            UiFx.ring(g, x + 14, headerY + 6, 8, 1, accent | 0xFF000000);
            UiFx.ring(g, x + 14, headerY + 6, 4, 1, 0xC0FFFFFF);
            g.text(mc.font,
                    Component.translatable("totemsexpansion.school." + school.getSerializedName())
                            .withStyle(school.chatColor, ChatFormatting.BOLD),
                    x + 28, headerY + 2, 0xFFFFFFFF);
        } else {
            g.text(mc.font,
                    Component.translatable("item.totemsexpansion.magic_totem")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    x + 8, headerY + 2, 0xFFFFFFFF);
        }
        // Level readout (right-aligned).
        int level = ClientMagicState.level();
        String levelStr = "lvl " + level;
        int lw = mc.font.width(levelStr);
        g.text(mc.font, Component.literal(levelStr)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                x + w - lw - 8, headerY + 2, 0xFFFFFFFF);

        // XP bar.
        long into = ClientMagicState.xpIntoLevel();
        long need = ClientMagicState.xpNeededForNext();
        float progress = ClientMagicState.progress();
        int barY = y + 22;
        UiFx.progressBar(g, x + 8, barY, w - 16, 8, progress,
                UiFx.brighten(accent, 0.2f) | 0xFF000000,
                UiFx.darken(accent, 0.6f) | 0xFF000000);

        String xpText = level >= Levels.MAX_LEVEL
                ? Component.translatable("totemsexpansion.ui.maxed").getString()
                : into + " / " + need + " XP";
        g.text(mc.font, Component.literal(xpText).withStyle(ChatFormatting.GRAY),
                x + 8, barY + 10, 0xFFBBBBBB);

        // Hint when no school chosen.
        if (schoolOpt.isEmpty()) {
            g.text(mc.font, Component.literal("→ Right-click the totem").withStyle(ChatFormatting.DARK_GRAY),
                    x + w - 112, barY + 10, 0xFF808080);
            drawLevelUpBanner(g);
            return;
        }

        // Ability pips row (only if school picked).
        MagicSchool school = schoolOpt.get();
        int pipsY = y + 44;
        int pipsGap = 4;
        int pipW = (w - 16 - pipsGap * 2) / 3;
        for (int slot = 0; slot < 3; slot++) {
            int px = x + 8 + slot * (pipW + pipsGap);
            drawAbilityPip(g, mc, px, pipsY, pipW, 32, school, slot, level, accent);
        }

        drawLevelUpBanner(g);
    }

    private static void drawAbilityPip(GuiGraphicsExtractor g, Minecraft mc,
                                       int x, int y, int w, int h,
                                       MagicSchool school, int slot, int level, int accent) {
        boolean unlocked = Levels.canUseAbility(level, slot);
        int rim = unlocked ? accent : 0xFF505060;
        int fillTop = unlocked ? UiFx.darken(accent, 0.3f) : 0xFF1A1A22;
        int fillBot = unlocked ? 0xFF0A0818 : 0xFF100E18;
        UiFx.fancyPanel(g, x, y, x + w, y + h, fillTop, fillBot, rim | 0xFF000000, rim | 0xFF000000);

        // Key tag.
        String key = switch (slot) { case 0 -> "Z"; case 1 -> "X"; default -> "C"; };
        g.text(mc.font, Component.literal(key)
                        .withStyle(ChatFormatting.BOLD, unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY),
                x + 4, y + 4, 0xFFFFFFFF);

        // Ability name (truncated).
        String name = Component.translatable(
                "totemsexpansion.ability." + school.getSerializedName() + "." + slot).getString();
        int maxW = w - 12;
        while (mc.font.width(name) > maxW && name.length() > 2) {
            name = name.substring(0, name.length() - 1);
        }
        g.text(mc.font, Component.literal(name)
                        .withStyle(unlocked ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY),
                x + 4, y + 16, 0xFFBBBBBB);

        // Tier indicator / lock.
        if (!unlocked) {
            int lx = x + w - 10, ly = y + 6;
            g.fill(lx - 3, ly + 2, lx + 3, ly + 6, 0xFF909090);
            g.fill(lx - 2, ly - 1, lx + 2, ly + 2, 0xFF909090);
            g.text(mc.font,
                    Component.literal(String.valueOf(Levels.unlockLevel(slot)))
                            .withStyle(ChatFormatting.DARK_RED),
                    x + w - 14, y + 18, 0xFF808080);
        }
    }

    private static void drawLevelUpBanner(GuiGraphicsExtractor g) {
        long now = System.currentTimeMillis();
        int banner = ClientMagicState.levelUpBannerValue(now);
        if (banner <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int cx = g.guiWidth() / 2;
        int yBase = g.guiHeight() / 2 - 100;
        String text = String.format(
                Component.translatable("totemsexpansion.ui.level_up").getString(), banner);
        int tw = mc.font.width(text);
        int w = tw + 60;
        int h = 34;
        int x = cx - w / 2;
        int y = yBase;

        // Outer glow pulse.
        int pulseA = 0x30 + (int)(Math.sin(now / 90.0) * 0x20);
        for (int k = 6; k >= 2; k--) {
            int a = Math.max(0, pulseA - (6 - k) * 0x08);
            g.fill(x - k, y - k, x + w + k, y + h + k, (a << 24) | 0xFFD060);
        }
        // Body.
        UiFx.fancyPanel(g, x, y, x + w, y + h, 0xFF3A1F60, 0xFF14081E,
                0xFFFFD060, 0xFFFFD060);
        // Stars left/right.
        drawBannerStar(g, x + 14, y + h / 2);
        drawBannerStar(g, x + w - 14, y + h / 2);
        // Text.
        g.centeredText(mc.font,
                Component.literal(text).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                cx, y + 8, 0xFFFFFFFF);
        g.centeredText(mc.font,
                Component.literal("★ LEVEL UP ★").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                cx, y + 20, 0xFFFFFFFF);
    }

    private static void drawBannerStar(GuiGraphicsExtractor g, int cx, int cy) {
        g.fill(cx - 1, cy - 4, cx + 2, cy + 5, 0xFFFFE060);
        g.fill(cx - 4, cy - 1, cx + 5, cy + 2, 0xFFFFE060);
        g.fill(cx - 3, cy - 3, cx - 1, cy - 1, 0xFFFFD060);
        g.fill(cx + 2, cy + 2, cx + 4, cy + 4, 0xFFFFD060);
    }

    private static boolean hasTotemAnywhere(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == MagicTotemItem.INSTANCE) {
                return true;
            }
        }
        return false;
    }
}
