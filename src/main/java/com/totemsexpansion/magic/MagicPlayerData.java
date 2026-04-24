package com.totemsexpansion.magic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.totemsexpansion.TotemsExpansionMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-player magic state saved on the overworld.
 *
 * <p>Holds, for each player UUID, the chosen magic school (once set it is permanent
 * for the run) and their total accumulated XP.</p>
 */
public class MagicPlayerData extends SavedData {
    public static final SavedDataType<MagicPlayerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, "magic_players"),
            MagicPlayerData::new,
            codec(),
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public record Entry(Optional<MagicSchool> school, long xp) {
        public static final Entry EMPTY = new Entry(Optional.empty(), 0L);

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                MagicSchool.CODEC.optionalFieldOf("school").forGetter(Entry::school),
                Codec.LONG.optionalFieldOf("xp", 0L).forGetter(Entry::xp)
        ).apply(i, Entry::new));

        public int level() { return Levels.levelFromTotalXp(xp); }
    }

    private record UuidEntry(UUID uuid, Entry entry) {
        static final Codec<UuidEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("uuid").forGetter(UuidEntry::uuid),
                Entry.CODEC.fieldOf("data").forGetter(UuidEntry::entry)
        ).apply(i, UuidEntry::new));
    }

    private final Map<UUID, Entry> players = new HashMap<>();

    public MagicPlayerData() {}

    private MagicPlayerData(List<UuidEntry> list) {
        for (UuidEntry e : list) players.put(e.uuid, e.entry);
    }

    private static Codec<MagicPlayerData> codec() {
        return RecordCodecBuilder.create(i -> i.group(
                UuidEntry.CODEC.listOf().optionalFieldOf("players", Collections.emptyList())
                        .forGetter(d -> d.players.entrySet().stream()
                                .map(en -> new UuidEntry(en.getKey(), en.getValue())).toList())
        ).apply(i, MagicPlayerData::new));
    }

    public static MagicPlayerData of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Entry get(UUID uuid) {
        return players.getOrDefault(uuid, Entry.EMPTY);
    }

    public void set(UUID uuid, Entry entry) {
        players.put(uuid, entry);
        setDirty();
    }

    public void setSchool(UUID uuid, MagicSchool school) {
        Entry cur = get(uuid);
        if (cur.school.isPresent()) return; // locked forever after pick
        set(uuid, new Entry(Optional.of(school), cur.xp));
    }

    public void addXp(UUID uuid, long amount) {
        if (amount <= 0) return;
        Entry cur = get(uuid);
        long newXp = cur.xp + amount;
        if (newXp < 0) newXp = Long.MAX_VALUE; // overflow guard
        set(uuid, new Entry(cur.school, newXp));
    }
}
