package com.totemsexpansion.magic;

/**
 * Level curve (rebalanced in v1.5.1 for reachability).
 *
 * <p>Cost to advance from level {@code N} to {@code N+1}: {@code 100 + 30·N}.</p>
 *
 * <p>Cumulative XP: {@code 100·L + 30·L·(L-1)/2}. Level 25 → ~11,500 XP
 * (≈ 1,150 zombies). Level 50 → ~41,750 XP. Level 100 → ~158,500 XP.</p>
 *
 * <p>The original ×3 exponential curve grew to ~4.2×10¹³ XP by level 25, which
 * was effectively unreachable; the new linear growth still feels progressive
 * but rewards normal mob grinding.</p>
 */
public final class Levels {
    /** Ability slot index thresholds. Slot 0 is free, slot 1 unlocks at 25, ultimate at 50. */
    public static final int UNLOCK_ABILITY_2 = 25;
    public static final int UNLOCK_ULTIMATE  = 50;

    /** Soft cap; well within long bounds for both per-level and cumulative costs. */
    public static final int MAX_LEVEL = 100;

    /** Base cost for the 0→1 step. */
    private static final long BASE_COST = 100L;
    /** Additive growth per level. */
    private static final long STEP_GROWTH = 30L;

    private Levels() {}

    /** Cost (in XP) to advance from level {@code fromLevel} to {@code fromLevel + 1}. */
    public static long costToLevel(int fromLevel) {
        if (fromLevel < 0) return BASE_COST;
        if (fromLevel >= MAX_LEVEL) return Long.MAX_VALUE;
        return BASE_COST + STEP_GROWTH * fromLevel;
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
