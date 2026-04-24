package com.totemsexpansion.client;

import com.totemsexpansion.magic.Levels;
import com.totemsexpansion.magic.MagicSchool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * "Spellbook" UI — shown when the Magic Totem is used after a school has been picked.
 * Displays current level, XP progress, and the three abilities with lock / unlock state.
 */
public class SpellbookScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int PANEL_H = 260;

    public SpellbookScreen() {
        super(Component.translatable("totemsexpansion.ui.spellbook_title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelTop = cy - PANEL_H / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("totemsexpansion.ui.close"),
                b -> this.onClose())
                .bounds(cx - 60, panelTop + PANEL_H - 28, 120, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int left = cx - PANEL_W / 2;
        int top = cy - PANEL_H / 2;

        drawBackdrop(g);
        drawPanel(g, left, top);

        MagicSchool school = ClientMagicState.school().orElse(MagicSchool.AIR);
        int accent = school.colorArgb;

        // Title bar — school name in themed color.
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.spellbook_title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                cx, top + 12, 0xFFFFFFFF);
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.school." + school.getSerializedName())
                        .withStyle(school.chatColor, ChatFormatting.ITALIC),
                cx, top + 28, 0xFFFFFFFF);

        // Level + XP row.
        int level = ClientMagicState.level();
        long into = ClientMagicState.xpIntoLevel();
        long need = ClientMagicState.xpNeededForNext();
        float progress = ClientMagicState.progress();

        g.text(this.font,
                Component.translatable("totemsexpansion.ui.level").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" "))
                        .append(Component.literal(String.valueOf(level)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)),
                left + 16, top + 54, 0xFFFFFFFF);

        // XP bar — 340 wide.
        int barX = left + 16;
        int barY = top + 70;
        int barW = PANEL_W - 32;
        int barH = 12;
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1030);
        int fillW = (int) (barW * progress);
        if (fillW > 0) {
            // Multi-layer gradient fill for bling.
            g.fill(barX, barY, barX + fillW, barY + barH, accent | 0xFF000000);
            g.fill(barX, barY, barX + fillW, barY + 3, brighten(accent, 0.35f) | 0xFF000000);
            g.fill(barX, barY + barH - 2, barX + fillW, barY + barH, darken(accent, 0.6f) | 0xFF000000);
        }
        // Animated shimmer.
        long now = System.currentTimeMillis();
        int sh = (int) ((now / 8) % (barW * 2));
        if (fillW > 0) {
            int x0 = Math.max(barX, barX + sh - barW);
            int x1 = Math.min(barX + fillW, barX + sh);
            if (x1 > x0) g.fill(x0, barY, x1, barY + barH, 0x40FFFFFF);
        }

        String progressText;
        if (level >= Levels.MAX_LEVEL) {
            progressText = Component.translatable("totemsexpansion.ui.maxed").getString();
        } else {
            progressText = String.format(Component.translatable("totemsexpansion.ui.xp_progress").getString(),
                    into, need);
        }
        g.centeredText(this.font, Component.literal(progressText).withStyle(ChatFormatting.WHITE),
                cx, barY + 14, 0xFFFFFFFF);

        // Ability rows.
        int rowY = top + 108;
        for (int slot = 0; slot < 3; slot++) {
            drawAbilityRow(g, left + 12, rowY + slot * 34, PANEL_W - 24, 30, school, slot, level);
        }

        // Hint.
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.bind_hint").withStyle(ChatFormatting.DARK_GRAY),
                cx, top + PANEL_H - 44, 0xFFBBBBBB);

        super.extractRenderState(g, mx, my, delta);
    }

    private void drawAbilityRow(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                MagicSchool school, int slot, int level) {
        boolean unlocked = Levels.canUseAbility(level, slot);
        int rim = unlocked ? school.colorArgb : 0xFF505050;
        int fill = unlocked ? darken(school.colorArgb, 0.3f) | 0xFF000000 : 0xFF1E1E24;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, rim | 0xFF000000);
        g.fill(x, y, x + w, y + h, fill);

        // Icon square on the left (colored dot / runes)
        int ix = x + 6, iy = y + 4, iw = h - 8;
        g.fill(ix, iy, ix + iw, iy + iw, (unlocked ? school.colorArgb : 0xFF808080) | 0xFF000000);
        g.fill(ix + 1, iy + 1, ix + iw - 1, iy + iw - 1, unlocked ? 0xFF1A1A1A : 0xFF0F0F12);
        // Inner rune.
        int rx = ix + iw / 2, ry = iy + iw / 2;
        int markColor = unlocked ? 0xFFFFFFFF : 0xFF707070;
        switch (slot) {
            case 0 -> g.fill(rx - 3, ry, rx + 4, ry + 1, markColor);
            case 1 -> {
                g.fill(rx - 3, ry - 3, rx + 4, ry - 2, markColor);
                g.fill(rx - 3, ry + 2, rx + 4, ry + 3, markColor);
            }
            default -> {
                g.fill(rx - 3, ry - 3, rx + 4, ry - 2, markColor);
                g.fill(rx, ry, rx + 1, ry + 1, markColor);
                g.fill(rx - 3, ry + 2, rx + 4, ry + 3, markColor);
            }
        }

        // Key binding tag.
        String key = switch (slot) { case 0 -> "Z"; case 1 -> "X"; default -> "C"; };
        int kx = x + w - 28;
        g.fill(kx, y + 4, kx + 22, y + h - 4, 0xFF000000);
        g.fill(kx + 1, y + 5, kx + 21, y + h - 5, unlocked ? (school.colorArgb | 0xFF000000) : 0xFF3A3A40);
        g.centeredText(this.font, Component.literal(key)
                        .withStyle(ChatFormatting.BOLD, unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY),
                kx + 11, y + 10, 0xFFFFFFFF);

        // Text — ability name + subtitle.
        int tx = ix + iw + 8;
        String abilityKey = "totemsexpansion.ability." + school.getSerializedName() + "." + slot;
        g.text(this.font,
                Component.translatable(abilityKey)
                        .withStyle(unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                tx, y + 4, 0xFFFFFFFF);
        Component status = unlocked
                ? Component.translatable("totemsexpansion.ui.unlocked").withStyle(ChatFormatting.GREEN)
                : Component.literal(String.format(
                        Component.translatable("totemsexpansion.ui.locked_at").getString(),
                        Levels.unlockLevel(slot)))
                .withStyle(ChatFormatting.RED);
        g.text(this.font, status, tx, y + 17, 0xFFFFFFFF);
    }

    private void drawBackdrop(GuiGraphicsExtractor g) {
        g.fill(0, 0, this.width, this.height, 0xFF04041A);
        int cx = this.width / 2, cy = this.height / 2;
        for (int k = 0; k < 10; k++) {
            int r = 280 - k * 26;
            int alpha = 0x03 + k * 0x02;
            int col = (alpha << 24) | 0x381070;
            g.fill(cx - r, cy - r, cx + r, cy + r, col);
        }
    }

    private void drawPanel(GuiGraphicsExtractor g, int left, int top) {
        for (int k = 7; k >= 2; k--) {
            int alpha = 0x05 + (7 - k) * 0x0E;
            g.fill(left - k, top - k, left + PANEL_W + k, top + PANEL_H + k,
                    (alpha << 24) | 0x6020D0);
        }
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF000000);
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFFDDAA55);
        g.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF1A1228);
        // Book spine on the left.
        g.fill(left + 6, top + 6, left + 12, top + PANEL_H - 6, 0xFF8A5A20);
        g.fill(left + 4, top + 6, left + 6, top + PANEL_H - 6, 0xFF603C10);
    }

    @Override public boolean isPauseScreen() { return false; }

    private static int darken(int argb, float f) {
        int r = (argb >>> 16) & 0xFF, gg = (argb >>> 8) & 0xFF, b = argb & 0xFF;
        r = (int) (r * f); gg = (int) (gg * f); b = (int) (b * f);
        return (r << 16) | (gg << 8) | b;
    }
    private static int brighten(int argb, float f) {
        int r = (argb >>> 16) & 0xFF, gg = (argb >>> 8) & 0xFF, b = argb & 0xFF;
        r = Math.min(0xFF, (int) (r + (0xFF - r) * f));
        gg = Math.min(0xFF, (int) (gg + (0xFF - gg) * f));
        b = Math.min(0xFF, (int) (b + (0xFF - b) * f));
        return (r << 16) | (gg << 8) | b;
    }
}
