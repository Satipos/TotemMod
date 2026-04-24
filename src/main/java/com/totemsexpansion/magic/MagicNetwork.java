package com.totemsexpansion.magic;

import com.totemsexpansion.TotemsExpansionMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Payloads for the Magic Totem system.
 *
 * <ul>
 *   <li>{@link OpenSchoolScreenPayload} — S2C, tells the client to open the school pick UI.</li>
 *   <li>{@link OpenSpellbookPayload} — S2C, tells the client to open the spellbook UI.</li>
 *   <li>{@link SyncStatePayload} — S2C, pushes the player's current school / XP to the client
 *       for HUD rendering and for the spellbook screen.</li>
 *   <li>{@link PickSchoolPayload} — C2S, the player picked a school in the UI.</li>
 *   <li>{@link CastAbilityPayload} — C2S, the player pressed an ability keybind.</li>
 * </ul>
 */
public final class MagicNetwork {
    private MagicNetwork() {}

    public static Identifier id(String p) {
        return Identifier.fromNamespaceAndPath(TotemsExpansionMod.MOD_ID, p);
    }

    // --- S2C ------------------------------------------------------------

    public record OpenSchoolScreenPayload() implements CustomPacketPayload {
        public static final Type<OpenSchoolScreenPayload> TYPE =
                new Type<>(id("open_school_screen"));
        public static final StreamCodec<FriendlyByteBuf, OpenSchoolScreenPayload> CODEC =
                StreamCodec.unit(new OpenSchoolScreenPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenSpellbookPayload() implements CustomPacketPayload {
        public static final Type<OpenSpellbookPayload> TYPE =
                new Type<>(id("open_spellbook"));
        public static final StreamCodec<FriendlyByteBuf, OpenSpellbookPayload> CODEC =
                StreamCodec.unit(new OpenSpellbookPayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SyncStatePayload(int schoolOrdinalOrMinusOne, long xp) implements CustomPacketPayload {
        public static final Type<SyncStatePayload> TYPE = new Type<>(id("sync_state"));
        public static final StreamCodec<FriendlyByteBuf, SyncStatePayload> CODEC =
                CustomPacketPayload.codec(SyncStatePayload::write, SyncStatePayload::new);

        public SyncStatePayload(FriendlyByteBuf buf) {
            this(buf.readVarInt() - 1, buf.readVarLong());
        }
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(schoolOrdinalOrMinusOne + 1);
            buf.writeVarLong(xp);
        }
        public Optional<MagicSchool> school() {
            return schoolOrdinalOrMinusOne < 0
                    ? Optional.empty()
                    : Optional.of(MagicSchool.values()[schoolOrdinalOrMinusOne]);
        }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record LevelUpPayload(int newLevel) implements CustomPacketPayload {
        public static final Type<LevelUpPayload> TYPE = new Type<>(id("level_up"));
        public static final StreamCodec<FriendlyByteBuf, LevelUpPayload> CODEC =
                CustomPacketPayload.codec(LevelUpPayload::write, LevelUpPayload::new);
        public LevelUpPayload(FriendlyByteBuf buf) { this(buf.readVarInt()); }
        public void write(FriendlyByteBuf buf) { buf.writeVarInt(newLevel); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // --- C2S ------------------------------------------------------------

    public record PickSchoolPayload(int ordinal) implements CustomPacketPayload {
        public static final Type<PickSchoolPayload> TYPE = new Type<>(id("pick_school"));
        public static final StreamCodec<FriendlyByteBuf, PickSchoolPayload> CODEC =
                CustomPacketPayload.codec(PickSchoolPayload::write, PickSchoolPayload::new);
        public PickSchoolPayload(FriendlyByteBuf buf) { this(buf.readVarInt()); }
        public void write(FriendlyByteBuf buf) { buf.writeVarInt(ordinal); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CastAbilityPayload(int slot) implements CustomPacketPayload {
        public static final Type<CastAbilityPayload> TYPE = new Type<>(id("cast_ability"));
        public static final StreamCodec<FriendlyByteBuf, CastAbilityPayload> CODEC =
                CustomPacketPayload.codec(CastAbilityPayload::write, CastAbilityPayload::new);
        public CastAbilityPayload(FriendlyByteBuf buf) { this(buf.readVarInt()); }
        public void write(FriendlyByteBuf buf) { buf.writeVarInt(slot); }
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(OpenSchoolScreenPayload.TYPE, OpenSchoolScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenSpellbookPayload.TYPE, OpenSpellbookPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncStatePayload.TYPE, SyncStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LevelUpPayload.TYPE, LevelUpPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PickSchoolPayload.TYPE, PickSchoolPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CastAbilityPayload.TYPE, CastAbilityPayload.CODEC);
    }
}
