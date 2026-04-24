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
 * v1.6.0 redesign — full-screen backdrop, three large school plates with circular
 * insignia, animated halos on hover, gradient nameplates, and ability preview rows.
 */
public class SchoolSelectScreen extends Screen {
    private static final int CARD_W = 170;
    private static final int CARD_H = 260;
    private static final int CARD_GAP = 22;

    public SchoolSelectScreen() {
        super(Component.translatable("totemsexpansion.ui.select_title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int cardY = cy - CARD_H / 2 + 20;
        int cardsTotal = 3 * CARD_W + 2 * CARD_GAP;
        int cardsLeft = cx - cardsTotal / 2;

        this.addRenderableWidget(new SchoolCard(cardsLeft, cardY, MagicSchool.AIR));
        this.addRenderableWidget(new SchoolCard(cardsLeft + (CARD_W + CARD_GAP), cardY, MagicSchool.EARTH));
        this.addRenderableWidget(new SchoolCard(cardsLeft + 2 * (CARD_W + CARD_GAP), cardY, MagicSchool.FIRE));

        this.addRenderableWidget(Button.builder(
                Component.translatable("totemsexpansion.ui.close"),
                b -> this.onClose())
                .bounds(cx - 60, cardY + CARD_H + 20, 120, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        UiFx.cosmicBackdrop(g, this.width, this.height, 0x6020D0, 0x5A5ACCL);

        int cx = this.width / 2;
        int cy = this.height / 2;
        int cardY = cy - CARD_H / 2 + 20;

        // Title "Choose Your School" with drop shadow and gold glow.
        Component title = Component.translatable("totemsexpansion.ui.select_title")
                .withStyle(ChatFormatting.BOLD);
        int titleY = cardY - 80;
        // Shadow.
        g.centeredText(this.font, Component.literal(title.getString()).withStyle(ChatFormatting.DARK_GRAY),
                cx + 2, titleY + 2, 0xFF000000);
        // Main title (scaled visually via brightening).
        g.centeredText(this.font, title.copy().withStyle(ChatFormatting.GOLD),
                cx, titleY, 0xFFFFFFFF);
        // Subtitle.
        g.centeredText(this.font,
                Component.translatable("totemsexpansion.ui.select_subtitle")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY),
                cx, titleY + 14, 0xFFBBBBBB);

        // Animated divider.
        long now = System.currentTimeMillis();
        int divY = titleY + 30;
        int divW = 420;
        for (int i = 0; i < divW; i++) {
            float t = ((i + now / 8f) % divW) / (float) divW;
            int alpha = (int) (0x60 + Math.sin(t * Math.PI * 2) * 0x80);
            alpha = Math.max(0x10, Math.min(0xFF, alpha));
            int col = (alpha << 24) | 0xB24CFF;
            g.fill(cx - divW / 2 + i, divY, cx - divW / 2 + i + 1, divY + 1, col);
        }
        // Shining dots at edges.
        g.fill(cx - divW / 2 - 3, divY - 1, cx - divW / 2 + 3, divY + 2, 0xFFFFD060);
        g.fill(cx + divW / 2 - 3, divY - 1, cx + divW / 2 + 3, divY + 2, 0xFFFFD060);

        super.extractRenderState(g, mx, my, delta);
    }

    @Override public boolean isPauseScreen() { return false; }

    private class SchoolCard extends AbstractWidget {
        private final MagicSchool school;
        private float hoverAnim = 0f;

        SchoolCard(int x, int y, MagicSchool school) {
            super(x, y, CARD_W, CARD_H, Component.literal(school.enName));
            this.school = school;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            boolean hover = this.isHovered();
            hoverAnim = Math.max(0, Math.min(1, hoverAnim + (hover ? 0.12f : -0.08f)));

            int x = this.getX(), y = this.getY(), w = this.getWidth(), h = this.getHeight();
            int accent = school.colorArgb;

            // Outer halo (grows on hover).
            int halo = (int) (12 * hoverAnim);
            for (int k = halo + 4; k >= 2; k--) {
                int a = Math.max(0, 0x08 + (halo + 4 - k) * 0x10);
                g.fill(x - k, y - k, x + w + k, y + h + k, (a << 24) | (accent & 0xFFFFFF));
            }

            // Main card — deep gradient body + themed top strip.
            int topFill = UiFx.darken(accent, 0.22f);
            int botFill = 0xFF0A0818;
            UiFx.fancyPanel(g, x, y, x + w, y + h, topFill, botFill, accent | 0xFF000000, accent | 0xFF000000);

            // Top ribbon — slightly darker header for contrast behind insignia.
            UiFx.vGradient(g, x + 2, y + 4, x + w - 2, y + 100, UiFx.darken(accent, 0.35f), UiFx.darken(accent, 0.12f));

            // Insignia centred in header.
            int gcx = x + w / 2;
            int gcy = y + 54;
            UiFx.schoolInsignia(g, gcx, gcy, 34, school, System.currentTimeMillis() % 100000L);

            // Pulsing ring when hovered.
            if (hoverAnim > 0.05f) {
                long now = System.currentTimeMillis();
                int pulseR = 38 + (int) ((Math.sin(now / 120.0) + 1) * 4);
                int a = (int) (0x60 * hoverAnim);
                UiFx.ring(g, gcx, gcy, pulseR, 1, (a << 24) | (accent & 0xFFFFFF));
            }

            // Divider below insignia.
            g.fill(x + 12, y + 100, x + w - 12, y + 101, UiFx.withAlpha(accent, 0xC0));

            // School name — large, bold.
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.translatable("totemsexpansion.school." + school.getSerializedName())
                            .withStyle(school.chatColor, ChatFormatting.BOLD),
                    gcx, y + 108, 0xFFFFFFFF);

            // Tagline — muted.
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.translatable("totemsexpansion.school." + school.getSerializedName() + ".tagline")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    gcx, y + 124, 0xFFBBBBBB);

            // Ability rows (key tag + name + icon).
            int rowY = y + 146;
            for (int i = 0; i < 3; i++) {
                drawAbilityPreview(g, x + 10, rowY + i * 26, w - 20, 22, i);
            }

            // Pick ribbon at the bottom.
            int btnY = y + h - 26;
            int ribbonColor = hover ? accent : UiFx.darken(accent, 0.4f);
            g.fill(x + 8, btnY, x + w - 8, btnY + 18, ribbonColor | 0xFF000000);
            g.fill(x + 8, btnY, x + w - 8, btnY + 2, UiFx.brighten(accent, 0.4f) | 0xFF000000);
            g.fill(x + 8, btnY + 16, x + w - 8, btnY + 18, UiFx.darken(accent, 0.5f) | 0xFF000000);
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.translatable("totemsexpansion.ui.pick")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    gcx, btnY + 5, 0xFFFFFFFF);
        }

        private void drawAbilityPreview(GuiGraphicsExtractor g, int x, int y, int w, int h, int slot) {
            int accent = school.colorArgb;
            // Key chip.
            String key = switch (slot) { case 0 -> "Z"; case 1 -> "X"; default -> "C"; };
            int chipW = 18;
            UiFx.vGradient(g, x, y, x + chipW, y + h, UiFx.darken(accent, 0.5f), UiFx.darken(accent, 0.2f));
            g.fill(x, y, x + chipW, y + 1, UiFx.brighten(accent, 0.3f) | 0xFF000000);
            g.fill(x, y + h - 1, x + chipW, y + h, UiFx.darken(accent, 0.6f) | 0xFF000000);
            g.centeredText(SchoolSelectScreen.this.font,
                    Component.literal(key).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD),
                    x + chipW / 2, y + 7, 0xFFFFFFFF);
            // Ability name.
            String key2 = "totemsexpansion.ability." + school.getSerializedName() + "." + slot;
            ChatFormatting tone = switch (slot) {
                case 0 -> ChatFormatting.WHITE;
                case 1 -> ChatFormatting.AQUA;
                default -> ChatFormatting.GOLD;
            };
            g.text(SchoolSelectScreen.this.font,
                    Component.translatable(key2).withStyle(tone),
                    x + chipW + 6, y + 7, 0xFFFFFFFF);
            // Tier hint.
            String hint = switch (slot) { case 0 -> "lvl 1"; case 1 -> "lvl 25"; default -> "lvl 50"; };
            g.text(SchoolSelectScreen.this.font,
                    Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY),
                    x + w - SchoolSelectScreen.this.font.width(hint) - 2, y + 7, 0xFF808080);
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
}
