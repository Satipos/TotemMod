package com.totemsexpansion.client;

import com.totemsexpansion.magic.MagicNetwork;
import com.totemsexpansion.magic.MagicSchool;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * First-pick UI for the Magic Totem — the player chooses one of three schools (permanent).
 *
 * <p>All rendering is done via {@link GuiGraphicsExtractor#fill(int, int, int, int, int)}
 * and its text/centeredText helpers; no textures. The backdrop is a slow-drifting
 * starfield and each school has a themed "rune" glyph drawn procedurally.</p>
 */
public class SchoolSelectScreen extends Screen {
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 300;
    private static final int CARD_W  = 150;
    private static final int CARD_H  = 200;
    private static final int CARD_GAP = 12;

    public SchoolSelectScreen() {
        super(Component.translatable("totemsexpansion.ui.select_title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelTop = cy - PANEL_H / 2;
        int cardY = panelTop + 60;
        int cardsTotal = 3 * CARD_W + 2 * CARD_GAP;
        int cardsLeft = cx - cardsTotal / 2;

        this.addRenderableWidget(new SchoolCard(cardsLeft, cardY, MagicSchool.AIR));
        this.addRenderableWidget(new SchoolCard(cardsLeft + (CARD_W + CARD_GAP), cardY, MagicSchool.EARTH));
        this.addRenderableWidget(new SchoolCard(cardsLeft + 2 * (CARD_W + CARD_GAP), cardY, MagicSchool.FIRE));

        this.addRenderableWidget(Button.builder(
                Component.translatable("totemsexpansion.ui.close"),
                b -> this.onClose())
                .bounds(cx - 60, panelTop + PANEL_H - 28, 120, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        drawBackdrop(g);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int panelTop = cy - PANEL_H / 2;
        int panelLeft = cx - PANEL_W / 2;
        drawPanel(g, panelLeft, panelTop);

        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.select_title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                cx, panelTop + 14, 0xFFFFFFFF);
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.select_subtitle")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                cx, panelTop + 30, 0xFFBBBBBB);

        // Animated underline divider.
        long now = System.currentTimeMillis();
        for (int i = 0; i < PANEL_W - 80; i++) {
            float t = (i + (now / 10f) % PANEL_W) / (float) PANEL_W;
            float a = (float) (0.4 + 0.6 * Math.sin(t * Math.PI * 2));
            int alpha = Math.min(0xFF, (int) (0x60 + a * 0x9F));
            int col = (alpha << 24) | 0xB24CFF;
            g.fill(panelLeft + 40 + i, panelTop + 46, panelLeft + 40 + i + 1, panelTop + 48, col);
        }

        super.extractRenderState(g, mx, my, delta);
    }

    private void drawBackdrop(GuiGraphicsExtractor g) {
        g.fill(0, 0, this.width, this.height, 0xFF04041A);
        int cx = this.width / 2, cy = this.height / 2;
        // Soft glow under panel.
        for (int k = 0; k < 10; k++) {
            int r = 260 - k * 24;
            int alpha = 0x05 + k * 0x03;
            int col = (alpha << 24) | 0x381070;
            g.fill(cx - r, cy - r, cx + r, cy + r, col);
        }
        // Drifting motes.
        long now = System.currentTimeMillis();
        for (int i = 0; i < 70; i++) {
            double t = (now / 30.0 + i * 37) % 1024;
            int mx = (int) ((i * 79 + t) % this.width);
            int my = (int) ((i * 53 + t * 0.5) % this.height);
            int col = switch (i % 3) {
                case 0 -> 0x9F60FF;
                case 1 -> 0xFFE080;
                default -> 0x60FFD0;
            };
            int a = Math.max(0, Math.min(0xFF, 0x40 + (int) (Math.sin(t * 0.2) * 0x40)));
            g.fill(mx, my, mx + 1, my + 1, (a << 24) | col);
        }
    }

    private void drawPanel(GuiGraphicsExtractor g, int left, int top) {
        // Outer glow layers.
        for (int k = 7; k >= 2; k--) {
            int alpha = 0x05 + (7 - k) * 0x0E;
            g.fill(left - k, top - k, left + PANEL_W + k, top + PANEL_H + k,
                    (alpha << 24) | 0x6020D0);
        }
        g.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF000000);
        g.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, 0xFFB24CFF);
        for (int r = 0; r < PANEL_H / 2; r++) {
            int base = 0x16;
            int v = (int) (base + (1.0 - (double) r / (PANEL_H / 2.0)) * 0x24);
            int col = (0xFF << 24) | (v << 16) | 0x001030;
            g.fill(left, top + r, left + PANEL_W, top + r + 1, col);
            g.fill(left, top + PANEL_H - r - 1, left + PANEL_W, top + PANEL_H - r, col);
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    private class SchoolCard extends AbstractWidget {
        private final MagicSchool school;

        SchoolCard(int x, int y, MagicSchool school) {
            super(x, y, CARD_W, CARD_H, Component.literal(school.enName));
            this.school = school;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hover = this.isHovered();
            int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();

            int rim = school.colorArgb;
            int baseA = hover ? 0xFF : 0xE0;
            int fillBase = hover ? darken(rim, 0.25f) : darken(rim, 0.65f);

            // Outer glow when hovered.
            if (hover) {
                for (int k = 6; k >= 2; k--) {
                    int a = 0x08 + (6 - k) * 0x14;
                    g.fill(x - k, y - k, x + w + k, y + h + k, (a << 24) | (rim & 0xFFFFFF));
                }
            }
            // Border + inner fill.
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, rim | 0xFF000000);
            g.fill(x, y, x + w, y + h, fillBase | 0xFF000000);

            // Top accent strip.
            g.fill(x, y, x + w, y + 4, rim | 0xFF000000);
            // Bottom accent strip.
            g.fill(x, y + h - 4, x + w, y + h, rim | 0xFF000000);

            // Large procedural glyph in the center-top.
            int gcx = x + w / 2;
            int gcy = y + 52;
            drawGlyph(g, gcx, gcy, school, hover);

            // Title.
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.translatable("totemsexpansion.school." + school.getSerializedName())
                            .withStyle(school.chatColor, ChatFormatting.BOLD),
                    gcx, y + 92, 0xFFFFFFFF);
            // Tagline.
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.translatable("totemsexpansion.school." + school.getSerializedName() + ".tagline")
                            .withStyle(ChatFormatting.GRAY),
                    gcx, y + 106, 0xFFDDDDDD);

            // Ability preview.
            int ly = y + 130;
            for (int i = 0; i < 3; i++) {
                String key = "totemsexpansion.ability." + school.getSerializedName() + "." + i;
                ChatFormatting c = switch (i) {
                    case 0 -> ChatFormatting.WHITE;
                    case 1 -> ChatFormatting.AQUA;
                    default -> ChatFormatting.GOLD;
                };
                String keyLabel = switch (i) { case 0 -> "[Z]"; case 1 -> "[X]"; default -> "[C]"; };
                g.text(SchoolSelectScreen.this.font,
                        Component.literal(keyLabel + " ").withStyle(ChatFormatting.DARK_GRAY)
                                .append(Component.translatable(key).withStyle(c)),
                        x + 8, ly + i * 12, 0xFFEEEEEE);
            }

            // Pick button hint at bottom.
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.translatable("totemsexpansion.ui.pick")
                            .withStyle(hover ? ChatFormatting.GOLD : ChatFormatting.GRAY, ChatFormatting.BOLD),
                    gcx, y + h - 16, 0xFFFFFFFF);
        }

        @Override
        public void onClick(net.minecraft.client.input.MouseButtonEvent ev, boolean doubled) {
            ClientPlayNetworking.send(new MagicNetwork.PickSchoolPayload(school.ordinal()));
            SchoolSelectScreen.this.onClose();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            this.defaultButtonNarrationText(out);
        }
    }

    /** Drift-animated rune glyph for each school (simple circular + shape combos). */
    private static void drawGlyph(GuiGraphicsExtractor g, int cx, int cy, MagicSchool s, boolean hover) {
        long now = System.currentTimeMillis();
        int accent = s.colorArgb;
        int soft = (accent & 0x00FFFFFF) | 0x80000000;

        // Concentric rings.
        for (int r = 14; r >= 6; r -= 2) {
            int alpha = hover ? 0xC0 : 0x80;
            int col = (alpha << 24) | (accent & 0xFFFFFF);
            drawRing(g, cx, cy, r, col);
        }
        switch (s) {
            case AIR -> {
                // Swirling arrow marks.
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(i * 90 + (now / 8.0) % 360);
                    int x = cx + (int) (Math.cos(a) * 18);
                    int y = cy + (int) (Math.sin(a) * 18);
                    g.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);
                }
            }
            case EARTH -> {
                // Square tiles.
                g.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0xFFFFFFFF);
                g.fill(cx - 10, cy - 10, cx - 6, cy - 6, soft);
                g.fill(cx + 6, cy - 10, cx + 10, cy - 6, soft);
                g.fill(cx - 10, cy + 6, cx - 6, cy + 10, soft);
                g.fill(cx + 6, cy + 6, cx + 10, cy + 10, soft);
            }
            case FIRE -> {
                // Flame bars radiating.
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(i * 45);
                    int x = cx + (int) (Math.cos(a) * 12);
                    int y = cy + (int) (Math.sin(a) * 12);
                    g.fill(x - 2, y - 2, x + 2, y + 2, 0xFFFFC040);
                }
            }
        }
    }

    private static void drawRing(GuiGraphicsExtractor g, int cx, int cy, int r, int col) {
        // 1-pixel-thick ring via sampled arcs.
        for (int a = 0; a < 360; a += 6) {
            double rad = Math.toRadians(a);
            int x = cx + (int) (Math.cos(rad) * r);
            int y = cy + (int) (Math.sin(rad) * r);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }

    private static int darken(int argb, float f) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int gg = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = (int) (r * f); gg = (int) (gg * f); b = (int) (b * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }
}
