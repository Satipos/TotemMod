package com.totemsexpansion.client;

import com.totemsexpansion.magic.MagicSchool;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared drawing helpers for the Magic UI — gradients, rounded panels, school insignia,
 * starfield backgrounds. All rendering is done with solid fills; no textures required.
 */
public final class UiFx {
    private UiFx() {}

    // ---- color utilities -------------------------------------------------

    public static int argb(int a, int r, int g, int b) { return (a << 24) | (r << 16) | (g << 8) | b; }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (a >>> 24) & 0xFF, ra = (a >>> 16) & 0xFF, ga = (a >>> 8) & 0xFF, ba = a & 0xFF;
        int ab = (b >>> 24) & 0xFF, rb = (b >>> 16) & 0xFF, gb = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ar = (int) (aa + (ab - aa) * t);
        int rr = (int) (ra + (rb - ra) * t);
        int gr = (int) (ga + (gb - ga) * t);
        int br = (int) (ba + (bb - ba) * t);
        return (ar << 24) | (rr << 16) | (gr << 8) | br;
    }

    public static int darken(int argb, float f) {
        int a = (argb >>> 24) & 0xFF, r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
        return (a << 24) | ((int)(r * f) << 16) | ((int)(g * f) << 8) | (int)(b * f);
    }

    public static int brighten(int argb, float f) {
        int a = (argb >>> 24) & 0xFF, r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;
        r = Math.min(0xFF, (int)(r + (0xFF - r) * f));
        g = Math.min(0xFF, (int)(g + (0xFF - g) * f));
        b = Math.min(0xFF, (int)(b + (0xFF - b) * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int argb, int a) { return (a << 24) | (argb & 0xFFFFFF); }

    // ---- fills -----------------------------------------------------------

    /** Horizontal gradient fill. */
    public static void hGradient(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int left, int right) {
        int w = x1 - x0;
        for (int i = 0; i < w; i++) {
            int c = lerp(left, right, (float) i / Math.max(1, w - 1));
            g.fill(x0 + i, y0, x0 + i + 1, y1, c);
        }
    }

    /** Vertical gradient fill. */
    public static void vGradient(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int top, int bottom) {
        int h = y1 - y0;
        for (int i = 0; i < h; i++) {
            int c = lerp(top, bottom, (float) i / Math.max(1, h - 1));
            g.fill(x0, y0 + i, x1, y0 + i + 1, c);
        }
    }

    /** Rounded-looking panel — corners are notched (skipped 1 pixel). */
    public static void roundedPanel(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int fill, int border) {
        // Drop corner pixels for a softer look.
        g.fill(x0 + 1, y0,     x1 - 1, y0 + 1, border); // top
        g.fill(x0 + 1, y1 - 1, x1 - 1, y1,     border); // bottom
        g.fill(x0,     y0 + 1, x0 + 1, y1 - 1, border); // left
        g.fill(x1 - 1, y0 + 1, x1,     y1 - 1, border); // right
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
    }

    /** Drop-shadow + rounded panel + inner highlight strip. */
    public static void fancyPanel(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1,
                                  int fillTop, int fillBottom, int border, int accentTop) {
        // Shadow layers (behind the panel).
        for (int k = 6; k >= 1; k--) {
            int a = 0x04 + (6 - k) * 0x06;
            g.fill(x0 - k, y0 - k, x1 + k, y1 + k, (a << 24) | 0x000000);
        }
        // Main panel.
        roundedPanel(g, x0, y0, x1, y1, fillTop, border);
        // Gradient body.
        vGradient(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, fillTop, fillBottom);
        // Top accent line.
        g.fill(x0 + 2, y0 + 1, x1 - 2, y0 + 3, accentTop);
        // Inner highlight at the very top.
        g.fill(x0 + 2, y0 + 3, x1 - 2, y0 + 4, withAlpha(brighten(accentTop, 0.4f), 0x80));
    }

    // ---- animated backdrops ---------------------------------------------

    /** Deep-space backdrop with drifting motes and a soft central glow. */
    public static void cosmicBackdrop(GuiGraphicsExtractor g, int width, int height,
                                      int glowColor, long seed) {
        g.fill(0, 0, width, height, 0xFF02021A);
        int cx = width / 2, cy = height / 2;
        for (int k = 0; k < 14; k++) {
            int r = Math.min(width, height) / 2 + 40 - k * 16;
            if (r <= 0) continue;
            int a = 0x03 + k * 0x02;
            g.fill(cx - r, cy - r, cx + r, cy + r, (a << 24) | (glowColor & 0xFFFFFF));
        }
        long now = System.currentTimeMillis();
        for (int i = 0; i < 110; i++) {
            double t = (now / 40.0 + i * (seed & 0xFF)) % 2048.0;
            int mx = (int) ((i * 83 + t * 0.7) % width);
            int my = (int) ((i * 59 + t * 0.4) % height);
            int col = switch (i % 4) {
                case 0 -> 0xB24CFF;
                case 1 -> 0xFFD060;
                case 2 -> 0x60C8FF;
                default -> 0xFFFFFF;
            };
            int a = Math.max(0x20, Math.min(0xF0, 0x50 + (int)(Math.sin(t * 0.18 + i) * 0x90)));
            g.fill(mx, my, mx + 1, my + 1, (a << 24) | col);
            if (i % 7 == 0) g.fill(mx + 1, my, mx + 2, my + 1, (a << 24) | col); // a few doubles
        }
    }

    // ---- school insignia -------------------------------------------------

    /**
     * Draw a large circular school insignia centred on {@code (cx, cy)} with outer
     * radius {@code R}. Accent color is taken from the school; all shapes are pure fills.
     */
    public static void schoolInsignia(GuiGraphicsExtractor g, int cx, int cy, int R, MagicSchool s, float animMs) {
        int accent = s.colorArgb;
        int soft = withAlpha(accent, 0x40);
        int mid = withAlpha(accent, 0x80);

        // Outer halo.
        for (int k = R + 8; k >= R; k--) {
            int a = Math.max(0, 0x40 - (k - R) * 0x08);
            fillCircle(g, cx, cy, k, (a << 24) | (accent & 0xFFFFFF));
        }
        // Main filled disk with vertical gradient.
        for (int r = R; r >= 0; r--) {
            float t = (float) r / R;
            int top = darken(accent, 0.25f);
            int bot = darken(accent, 0.6f);
            int col = lerp(top, bot, t);
            fillCircle(g, cx, cy, r, 0xFF000000 | (col & 0xFFFFFF));
        }
        // Inner ring.
        ring(g, cx, cy, R - 2, 1, 0xFF000000);
        ring(g, cx, cy, R - 4, 1, 0xC0FFFFFF);

        // School-specific glyph.
        switch (s) {
            case AIR -> drawAirGlyph(g, cx, cy, R, animMs);
            case EARTH -> drawEarthGlyph(g, cx, cy, R);
            case FIRE -> drawFireGlyph(g, cx, cy, R, animMs);
        }
    }

    private static void drawAirGlyph(GuiGraphicsExtractor g, int cx, int cy, int R, float animMs) {
        // Swirling tri-spoke.
        float rot = (animMs / 30f) % 360f;
        for (int i = 0; i < 3; i++) {
            float base = rot + i * 120f;
            for (int t = 0; t < 16; t++) {
                double a = Math.toRadians(base + t * 5);
                int rad = R - 6 - t / 2;
                if (rad < 2) break;
                int x = cx + (int) (Math.cos(a) * rad);
                int y = cy + (int) (Math.sin(a) * rad);
                int alpha = 0xFF - t * 10;
                g.fill(x - 1, y - 1, x + 1, y + 1, (alpha << 24) | 0xFFFFFF);
            }
        }
        // Central bright dot.
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
    }

    private static void drawEarthGlyph(GuiGraphicsExtractor g, int cx, int cy, int R) {
        // Mountain silhouette.
        int b = cy + R / 2;
        int w = R + 4;
        for (int i = -w / 2; i <= w / 2; i++) {
            int h = (int) (Math.max(0, (w / 2 - Math.abs(i))) * 1.2);
            g.fill(cx + i, b - h, cx + i + 1, b, 0xFF2F1808);
            if (h > 4) g.fill(cx + i, b - h, cx + i + 1, b - h + 1, 0xFFFFFFFF);
        }
        // Gem in the middle.
        int gy = cy - 6;
        g.fill(cx - 2, gy - 3, cx + 3, gy - 2, 0xFFFFFFFF);
        g.fill(cx - 3, gy - 2, cx + 4, gy + 2, 0xFFFFE090);
        g.fill(cx - 2, gy + 2, cx + 3, gy + 3, 0xFFC28020);
        g.fill(cx - 1, gy + 3, cx + 2, gy + 4, 0xFF703800);
    }

    private static void drawFireGlyph(GuiGraphicsExtractor g, int cx, int cy, int R, float animMs) {
        // Stylized flame silhouette.
        float wave = (float) Math.sin(animMs / 200.0) * 2;
        for (int y = -R + 4; y <= R - 4; y++) {
            float t = (y + R) / (float)(2 * R);
            int width = (int) (Math.sin(Math.PI * t) * (R - 2)) + (int)(wave * t);
            int col = lerp(0xFFFFF060, 0xFFFF3000, t);
            g.fill(cx - width, cy + y, cx + width + 1, cy + y + 1, col);
        }
        // Inner yellow core.
        for (int y = -R / 2; y <= R / 2 - 2; y++) {
            float t = (y + R / 2f) / (R / 2f);
            int width = (int) (Math.sin(Math.PI * t) * (R / 2));
            g.fill(cx - width / 2, cy + y, cx + width / 2 + 1, cy + y + 1, 0xFFFFFFC0);
        }
    }

    // ---- low-level geometry ---------------------------------------------

    public static void fillCircle(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        int r2 = r * r;
        for (int y = -r; y <= r; y++) {
            int span = (int) Math.sqrt(r2 - y * y);
            g.fill(cx - span, cy + y, cx + span + 1, cy + y + 1, color);
        }
    }

    public static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int thickness, int color) {
        for (int a = 0; a < 360; a += 2) {
            double rad = Math.toRadians(a);
            int x = cx + (int) (Math.cos(rad) * r);
            int y = cy + (int) (Math.sin(rad) * r);
            g.fill(x - thickness / 2, y - thickness / 2, x + (thickness + 1) / 2, y + (thickness + 1) / 2, color);
        }
    }

    /** Horizontal progress bar with gradient fill and shimmer animation. */
    public static void progressBar(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                   float progress, int colorStart, int colorEnd) {
        // Outer border.
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF000000);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF333344);
        // Track.
        vGradient(g, x, y, x + w, y + h, 0xFF0A0818, 0xFF120A28);
        // Fill.
        int fillW = (int) (w * Math.max(0, Math.min(1, progress)));
        if (fillW > 0) {
            hGradient(g, x, y, x + fillW, y + h, colorStart, colorEnd);
            // Top highlight.
            g.fill(x, y, x + fillW, y + 1, withAlpha(0xFFFFFFFF, 0x60));
            g.fill(x, y + 1, x + fillW, y + 2, withAlpha(0xFFFFFFFF, 0x30));
            // Shimmer sweep.
            long now = System.currentTimeMillis();
            int sh = (int) ((now / 10) % (w * 2)) - w;
            int x0 = Math.max(x, x + sh);
            int x1 = Math.min(x + fillW, x + sh + 14);
            if (x1 > x0) g.fill(x0, y, x1, y + h, withAlpha(0xFFFFFFFF, 0x40));
        }
    }
}
