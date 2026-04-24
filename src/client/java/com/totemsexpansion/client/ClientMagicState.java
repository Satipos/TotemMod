package com.totemsexpansion.client;

import com.totemsexpansion.magic.Levels;
import com.totemsexpansion.magic.MagicSchool;

import java.util.Optional;

/**
 * Singleton holding the current player's magic state as the client last heard it
 * from the server. Updated by {@code SyncStatePayload}, consumed by the HUD and
 * the spellbook screen.
 */
public final class ClientMagicState {
    private ClientMagicState() {}

    private static Optional<MagicSchool> school = Optional.empty();
    private static long xp = 0L;
    private static long lastLevelUpTickDisplayedUntil = 0L;
    private static int lastLevelUpValue = 0;

    public static void update(Optional<MagicSchool> s, long x) {
        school = s;
        xp = x;
    }

    public static Optional<MagicSchool> school() { return school; }
    public static long xp() { return xp; }
    public static int level() { return Levels.levelFromTotalXp(xp); }
    public static long xpIntoLevel() { return Levels.xpIntoLevel(xp); }
    public static long xpNeededForNext() { return Levels.costToLevel(level()); }
    public static float progress() { return Levels.progress(xp); }

    public static void onLevelUp(int newLevel, long systemTimeMs) {
        lastLevelUpValue = newLevel;
        lastLevelUpTickDisplayedUntil = systemTimeMs + 4000L;
    }

    public static int levelUpBannerValue(long now) {
        return now <= lastLevelUpTickDisplayedUntil ? lastLevelUpValue : 0;
    }
}
