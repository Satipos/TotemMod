package com.totemsexpansion.client;

import com.totemsexpansion.magic.Levels;
import com.totemsexpansion.magic.MagicSchool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * v1.6.0 spellbook — large insignia to the left, XP panel + level readout at top,
 * three ability cards laid out in a column with icons + key tags + unlock status.
 */
public class SpellbookScreen extends Screen {
    private static final int W = 440;
    private static final int H = 300;

    public SpellbookScreen() {
        super(Component.translatable("totemsexpansion.ui.spellbook_title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int top = cy - H / 2;
        this.addRenderableWidget(Button.builder(
                Component.translatable("totemsexpansion.ui.close"),
                b -> this.onClose())
                .bounds(cx - 60, top + H - 28, 120, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        MagicSchool school = ClientMagicState.school().orElse(MagicSchool.AIR);
        int accent = school.colorArgb;

        UiFx.cosmicBackdrop(g, this.width, this.height, accent & 0xFFFFFF, 0x3C3C3CL);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int left = cx - W / 2;
        int top = cy - H / 2;

        // Main panel.
        UiFx.fancyPanel(g, left, top, left + W, top + H,
                0xFF18102A, 0xFF0A0818, accent | 0xFF000000, accent | 0xFF000000);

        // Header bar — gradient strip containing spellbook title and school name.
        int headerH = 34;
        UiFx.vGradient(g, left + 1, top + 1, left + W - 1, top + headerH,
                UiFx.darken(accent, 0.5f), UiFx.darken(accent, 0.15f));
        // Title.
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.spellbook_title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                cx, top + 6, 0xFFFFFFFF);
        // School sub-label.
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.school." + school.getSerializedName())
                        .withStyle(school.chatColor, ChatFormatting.BOLD, ChatFormatting.ITALIC),
                cx, top + 20, 0xFFFFFFFF);

        // Left panel: large school insignia + level badge.
        int leftColX = left + 18, leftColY = top + headerH + 14;
        int leftColW = 120, leftColH = H - headerH - 50;
        drawLeftColumn(g, leftColX, leftColY, leftColW, leftColH, school, accent);

        // Right column: XP bar + ability rows.
        int rightX = leftColX + leftColW + 12;
        int rightY = leftColY;
        int rightW = left + W - rightX - 18;
        drawRightColumn(g, rightX, rightY, rightW, leftColH, school, accent);

        super.extractRenderState(g, mx, my, delta);
    }

    private void drawLeftColumn(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                MagicSchool school, int accent) {
        // Sub-panel.
        UiFx.fancyPanel(g, x, y, x + w, y + h, UiFx.darken(accent, 0.2f), 0xFF0A0818,
                UiFx.darken(accent, 0.7f) | 0xFF000000, accent | 0xFF000000);

        // Insignia.
        int cx = x + w / 2;
        int icy = y + 60;
        UiFx.schoolInsignia(g, cx, icy, 42, school, System.currentTimeMillis() % 100000L);

        // Divider.
        g.fill(x + 10, icy + 52, x + w - 10, icy + 53, UiFx.withAlpha(accent, 0xC0));

        // Level badge.
        int level = ClientMagicState.level();
        String levelNum = String.valueOf(level);
        int badgeY = icy + 64;
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.level")
                        .withStyle(ChatFormatting.GRAY),
                cx, badgeY, 0xFFBBBBBB);
        // Huge level number — faked by rendering twice (shadow + fill) with font.
        Component levelText = Component.literal(levelNum)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.centeredText(this.font, Component.literal(levelNum).withStyle(ChatFormatting.DARK_GRAY),
                cx + 1, badgeY + 11, 0xFF000000);
        g.centeredText(this.font, levelText, cx, badgeY + 10, 0xFFFFFFFF);

        // Stars row — one star per 10 levels up to 10 stars.
        int stars = Math.min(10, level / 10);
        int sy = badgeY + 28;
        int sx0 = cx - 45;
        for (int i = 0; i < 10; i++) {
            int sx = sx0 + i * 10;
            boolean filled = i < stars;
            drawStar(g, sx, sy, filled ? 0xFFFFD060 : 0xFF404050);
        }
    }

    private void drawRightColumn(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                 MagicSchool school, int accent) {
        int level = ClientMagicState.level();
        long into = ClientMagicState.xpIntoLevel();
        long need = ClientMagicState.xpNeededForNext();
        long total = ClientMagicState.xp();

        // XP header row.
        g.text(this.font,
                Component.translatable("totemsexpansion.ui.xp")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD),
                x, y, 0xFFBBBBBB);
        String progressStr = level >= Levels.MAX_LEVEL
                ? Component.translatable("totemsexpansion.ui.maxed").getString()
                : String.format(Component.translatable("totemsexpansion.ui.xp_progress").getString(),
                        into, need);
        int pw = this.font.width(progressStr);
        g.text(this.font, Component.literal(progressStr).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                x + w - pw, y, 0xFFFFFFFF);

        // XP bar.
        UiFx.progressBar(g, x, y + 14, w, 12, ClientMagicState.progress(),
                UiFx.brighten(accent, 0.2f) | 0xFF000000,
                UiFx.darken(accent, 0.6f) | 0xFF000000);

        // Total XP line.
        g.text(this.font,
                Component.literal(String.format("Total: %,d XP", total))
                        .withStyle(ChatFormatting.DARK_GRAY),
                x, y + 30, 0xFF808080);

        // Three ability rows.
        int rowY = y + 48;
        int rowH = 42;
        for (int slot = 0; slot < 3; slot++) {
            drawAbilityRow(g, x, rowY + slot * (rowH + 4), w, rowH, school, slot, level);
        }
    }

    private void drawAbilityRow(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                MagicSchool school, int slot, int level) {
        boolean unlocked = Levels.canUseAbility(level, slot);
        int accent = school.colorArgb;
        int rim = unlocked ? accent : 0xFF505060;
        int fillTop = unlocked ? UiFx.darken(accent, 0.3f) : 0xFF201E2C;
        int fillBot = unlocked ? 0xFF0E0A1A : 0xFF100E18;

        UiFx.fancyPanel(g, x, y, x + w, y + h, fillTop, fillBot, rim | 0xFF000000, rim | 0xFF000000);

        // Left icon block.
        int iconSize = h - 10;
        int ix = x + 6, iy = y + 5;
        g.fill(ix, iy, ix + iconSize, iy + iconSize, unlocked ? UiFx.darken(accent, 0.1f) | 0xFF000000 : 0xFF1A1A22);
        g.fill(ix, iy, ix + iconSize, iy + 1, rim | 0xFF000000);
        g.fill(ix, iy + iconSize - 1, ix + iconSize, iy + iconSize, rim | 0xFF000000);
        // Inner glyph.
        int mcx = ix + iconSize / 2, mcy = iy + iconSize / 2;
        drawSlotGlyph(g, mcx, mcy, slot, unlocked ? 0xFFFFFFFF : 0xFF707080);

        // Title + subtitle text.
        int tx = ix + iconSize + 10;
        String abilityKey = "totemsexpansion.ability." + school.getSerializedName() + "." + slot;
        g.text(this.font,
                Component.translatable(abilityKey)
                        .withStyle(unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY,
                                ChatFormatting.BOLD),
                tx, y + 6, 0xFFFFFFFF);

        String tier = switch (slot) {
            case 0 -> "Primary";
            case 1 -> "Secondary";
            default -> "ULTIMATE";
        };
        ChatFormatting tierColor = switch (slot) {
            case 0 -> ChatFormatting.GRAY;
            case 1 -> ChatFormatting.AQUA;
            default -> ChatFormatting.GOLD;
        };
        g.text(this.font, Component.literal(tier)
                        .withStyle(tierColor, unlocked ? ChatFormatting.ITALIC : ChatFormatting.STRIKETHROUGH),
                tx, y + 18, 0xFFAAAAAA);

        Component status = unlocked
                ? Component.translatable("totemsexpansion.ui.unlocked").withStyle(ChatFormatting.GREEN)
                : Component.literal(String.format(
                        Component.translatable("totemsexpansion.ui.locked_at").getString(),
                        Levels.unlockLevel(slot)))
                .withStyle(ChatFormatting.RED);
        g.text(this.font, status, tx, y + 29, 0xFFFFFFFF);

        // Right-side key chip.
        String key = switch (slot) { case 0 -> "Z"; case 1 -> "X"; default -> "C"; };
        int chipW = 30, chipH = h - 10;
        int kx = x + w - chipW - 6, ky = y + 5;
        UiFx.vGradient(g, kx, ky, kx + chipW, ky + chipH,
                unlocked ? UiFx.brighten(accent, 0.1f) : 0xFF303040,
                unlocked ? UiFx.darken(accent, 0.5f) : 0xFF1A1A22);
        g.fill(kx, ky, kx + chipW, ky + 1, unlocked ? UiFx.brighten(accent, 0.5f) | 0xFF000000 : 0xFF505060);
        g.fill(kx, ky + chipH - 1, kx + chipW, ky + chipH, unlocked ? UiFx.darken(accent, 0.7f) | 0xFF000000 : 0xFF202030);
        g.centeredText(this.font,
                Component.literal(key).withStyle(ChatFormatting.BOLD,
                        unlocked ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY),
                kx + chipW / 2, ky + chipH / 2 - 4, 0xFFFFFFFF);

        // Lock icon on closed rows.
        if (!unlocked) {
            int lx = x + w / 2, ly = y + h / 2;
            g.fill(lx - 4, ly - 1, lx + 5, ly + 5, 0xFFB0B0C0);
            g.fill(lx - 3, ly - 5, lx + 4, ly - 1, 0xFFB0B0C0);
            g.fill(lx - 2, ly - 4, lx + 3, ly - 1, 0xFF1A1A22);
            g.fill(lx, ly, lx + 1, ly + 3, 0xFF1A1A22);
        }
    }

    private void drawSlotGlyph(GuiGraphicsExtractor g, int cx, int cy, int slot, int color) {
        switch (slot) {
            case 0 -> {
                g.fill(cx - 5, cy - 1, cx + 6, cy + 1, color);
                g.fill(cx - 1, cy - 5, cx + 1, cy + 6, color);
            }
            case 1 -> {
                g.fill(cx - 6, cy - 1, cx + 7, cy + 1, color);
                g.fill(cx - 4, cy - 4, cx + 5, cy - 2, color);
                g.fill(cx - 4, cy + 2, cx + 5, cy + 4, color);
            }
            default -> {
                // Star shape.
                g.fill(cx - 1, cy - 6, cx + 1, cy + 6, color);
                g.fill(cx - 6, cy - 1, cx + 6, cy + 1, color);
                g.fill(cx - 4, cy - 4, cx - 2, cy - 2, color);
                g.fill(cx + 2, cy + 2, cx + 4, cy + 4, color);
                g.fill(cx - 4, cy + 2, cx - 2, cy + 4, color);
                g.fill(cx + 2, cy - 4, cx + 4, cy - 2, color);
            }
        }
    }

    private void drawStar(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.fill(cx - 1, cy - 3, cx + 2, cy + 3, color);
        g.fill(cx - 3, cy - 1, cx + 3, cy + 2, color);
    }

    @Override public boolean isPauseScreen() { return false; }
}
