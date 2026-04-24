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
 * Screen-space overlay shown whenever the player is holding a Magic Totem.
 * Renders current level + XP bar + 3 ability slot indicators with cooldown sweeps.
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
        if (!shouldShow(mc.player)) return;

        Optional<MagicSchool> schoolOpt = ClientMagicState.school();
        if (schoolOpt.isEmpty()) {
            drawNoSchoolHint(g);
            return;
        }
        MagicSchool school = schoolOpt.get();
        int accent = school.colorArgb;

        int w = 180;
        int h = 72;
        int x = g.guiWidth() - w - 8;
        int y = 8;

        // Outer glow.
        for (int k = 4; k >= 2; k--) {
            int alpha = 0x10 + (4 - k) * 0x14;
            g.fill(x - k, y - k, x + w + k, y + h + k, (alpha << 24) | (accent & 0xFFFFFF));
        }
        // Panel.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xDD0C0A1A);
        g.fill(x, y, x + w, y + 2, accent | 0xFF000000);

        int level = ClientMagicState.level();
        long into = ClientMagicState.xpIntoLevel();
        long need = ClientMagicState.xpNeededForNext();
        float progress = ClientMagicState.progress();

        // Title + level.
        g.text(mc.font,
                Component.translatable("totemsexpansion.school." + school.getSerializedName())
                        .withStyle(school.chatColor, ChatFormatting.BOLD),
                x + 8, y + 6, 0xFFFFFFFF);
        g.text(mc.font,
                Component.translatable("totemsexpansion.ui.level").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" "))
                        .append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
                x + w - 8 - mc.font.width(Component.translatable("totemsexpansion.ui.level").getString() + " 99"),
                y + 6, 0xFFFFFFFF);

        // XP bar.
        int barX = x + 8, barY = y + 20, barW = w - 16, barH = 6;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1030);
        int fillW = (int) (barW * progress);
        if (fillW > 0) g.fill(barX, barY, barX + fillW, barY + barH, accent | 0xFF000000);
        String xpText = level >= Levels.MAX_LEVEL
                ? Component.translatable("totemsexpansion.ui.maxed").getString()
                : into + " / " + need + " " + Component.translatable("totemsexpansion.ui.xp").getString();
        g.text(mc.font, Component.literal(xpText).withStyle(ChatFormatting.DARK_GRAY),
                barX, barY + barH + 2, 0xFFBBBBBB);

        // Three ability cooldown pips.
        int pipY = y + 44;
        for (int slot = 0; slot < 3; slot++) {
            int pipX = x + 8 + slot * ((w - 16) / 3 + 0);
            drawAbilityPip(g, mc, pipX, pipY, (w - 24) / 3, 22, school, slot, level);
        }

        // Level-up banner.
        long now = System.currentTimeMillis();
        int banner = ClientMagicState.levelUpBannerValue(now);
        if (banner > 0) {
            drawLevelUpBanner(g, banner);
        }
    }

    private static void drawAbilityPip(GuiGraphicsExtractor g, Minecraft mc,
                                       int x, int y, int w, int h,
                                       MagicSchool school, int slot, int level) {
        boolean unlocked = Levels.canUseAbility(level, slot);
        int rim = unlocked ? school.colorArgb : 0xFF505055;
        int fill = unlocked ? 0xFF1A1622 : 0xFF0E0E14;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, rim | 0xFF000000);
        g.fill(x, y, x + w, y + h, fill);

        String key = switch (slot) { case 0 -> "Z"; case 1 -> "X"; default -> "C"; };
        g.text(mc.font, Component.literal(key)
                        .withStyle(ChatFormatting.BOLD, unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY),
                x + 4, y + 4, 0xFFFFFFFF);

        String shortName = Component.translatable(
                "totemsexpansion.ability." + school.getSerializedName() + "." + slot).getString();
        int maxChars = Math.max(3, (w - 14) / 6);
        if (shortName.length() > maxChars) shortName = shortName.substring(0, maxChars);
        g.text(mc.font, Component.literal(shortName)
                        .withStyle(unlocked ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY),
                x + 4, y + 14, 0xFFFFFFFF);

        if (!unlocked) {
            // Lock overlay.
            int cx = x + w - 10, cy = y + h / 2;
            g.fill(cx - 3, cy - 2, cx + 3, cy + 4, 0xFF808080);
            g.fill(cx - 2, cy - 5, cx + 2, cy - 2, 0xFF808080);
        }
    }

    private static void drawLevelUpBanner(GuiGraphicsExtractor g, int level) {
        Minecraft mc = Minecraft.getInstance();
        int cx = g.guiWidth() / 2;
        int y = g.guiHeight() / 2 - 80;
        String text = String.format(
                Component.translatable("totemsexpansion.ui.level_up").getString(), level);
        int w = mc.font.width(text) + 40;
        int x = cx - w / 2;
        g.fill(x - 2, y - 2, x + w + 2, y + 26, 0xFF000000);
        g.fill(x, y, x + w, y + 24, 0xFF2A1A4A);
        g.fill(x, y, x + w, y + 2, 0xFFFFD060);
        g.fill(x, y + 22, x + w, y + 24, 0xFFFFD060);
        g.centeredText(mc.font, Component.literal(text)
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                cx, y + 8, 0xFFFFFFFF);
    }

    private static void drawNoSchoolHint(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        int w = 200, h = 28;
        int x = g.guiWidth() - w - 8, y = 8;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xDD181230);
        g.fill(x, y, x + w, y + 2, 0xFFB24CFF);
        g.centeredText(mc.font,
                Component.translatable("item.totemsexpansion.magic_totem")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                x + w / 2, y + 6, 0xFFFFFFFF);
        g.centeredText(mc.font,
                Component.literal("Right-click to pick a school").withStyle(ChatFormatting.GRAY),
                x + w / 2, y + 17, 0xFFBBBBBB);
    }

    private static boolean shouldShow(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == MagicTotemItem.INSTANCE) {
                return true;
            }
        }
        return false;
    }
}
