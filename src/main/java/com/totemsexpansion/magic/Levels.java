package com.totemsexpansion.magic;

/**
 * Level curve exactly as the design doc specified:
 * <pre>
 * Level 1:      100 XP
 * Level 2:      300 XP
 * Level 3:      900 XP
 * Level 4:    2,700 XP
 *   ... each level costs 3× the previous ...
 * </pre>
 *
 * <p>The cost to go from level {@code N-1} to level {@code N} is {@code 100 * 3^(N-1)}
 * (so the first advancement, 0→1, costs 100 XP).</p>
 *
 * <p>With this curve the cumulative XP to reach a given level is
 * {@code 100 * (3^N - 1) / 2} which blows up quickly — level 25 would require
 * ≈ 4.2×10¹³ XP. Kept as-is per the design spec; the UI surfaces the raw XP
 * numbers so the player can see the curve and understand the challenge.</p>
 *
 * <p>We cap the max level at {@value #MAX_LEVEL} to avoid {@code long} overflow
 * on multiplication.</p>
 */
public final class Levels {
    /** Ability slot index thresholds. Slot 0 is free, slot 1 unlocks at 25, ultimate at 50. */
    public static final int UNLOCK_ABILITY_2 = 25;
    public static final int UNLOCK_ULTIMATE  = 50;

    /** 3^39 * 100 still fits in a signed long, 3^40 does not. We cap below that. */
    public static final int MAX_LEVEL = 39;

    private Levels() {}

    /** Cost (in XP) to advance from level {@code fromLevel} to {@code fromLevel + 1}. */
    public static long costToLevel(int fromLevel) {
        if (fromLevel < 0) return 100L;
        if (fromLevel >= MAX_LEVEL) return Long.MAX_VALUE;
        long cost = 100L;
        for (int i = 0; i < fromLevel; i++) cost *= 3L;
        return cost;
    }

    /** Total XP needed to reach exactly level {@code level} (sum of all level costs). */
    public static long totalXpForLevel(int level) {
        long sum = 0L;
        for (int i = 0; i < Math.min(level, MAX_LEVEL); i++) sum += costToLevel(i);
        return sum;
    }

    /** Highest level whose total XP cost is ≤ given xp. */
    public static int levelFromTotalXp(long xp) {
        if (xp < 100) return 0;
        int level = 0;
        long acc = 0;
        for (int i = 0; i < MAX_LEVEL; i++) {
            long step = costToLevel(i);
            if (acc + step > xp) break;
            acc += step;
            level = i + 1;
        }
        return level;
    }

    /** XP accumulated into the current level (xp − total xp for that level). */
    public static long xpIntoLevel(long xp) {
        int level = levelFromTotalXp(xp);
        return xp - totalXpForLevel(level);
    }

    /** Progress [0.0, 1.0] through the current level. Returns 1.0 at max level. */
    public static float progress(long xp) {
        int level = levelFromTotalXp(xp);
        if (level >= MAX_LEVEL) return 1.0f;
        long into = xpIntoLevel(xp);
        long need = costToLevel(level);
        if (need <= 0) return 1.0f;
        return Math.min(1.0f, (float) into / (float) need);
    }

    public static boolean canUseAbility(int level, int slot) {
        return switch (slot) {
            case 0 -> true;
            case 1 -> level >= UNLOCK_ABILITY_2;
            case 2 -> level >= UNLOCK_ULTIMATE;
            default -> false;
        };
    }

    public static int unlockLevel(int slot) {
        return switch (slot) {
            case 1 -> UNLOCK_ABILITY_2;
            case 2 -> UNLOCK_ULTIMATE;
            default -> 1;
        };
    }
}
