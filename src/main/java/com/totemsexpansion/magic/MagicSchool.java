package com.totemsexpansion.magic;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Three magic schools the player can pick from with the {@link MagicTotemItem}. */
public enum MagicSchool implements StringRepresentable {
    AIR("air",     0xFF7FD4FF, ChatFormatting.AQUA,  "Воздух",  "Air",   "Wind Gust",    "Sky Strike",     "Tempest Eye"),
    EARTH("earth", 0xFF8FB560, ChatFormatting.GREEN, "Земля",   "Earth", "Rock Burst",   "Stone Spike",    "Earthquake"),
    FIRE("fire",   0xFFFF7C3A, ChatFormatting.GOLD,  "Огонь",   "Fire",  "Fireball",     "Flame Breath",   "Meteor");

    public static final Codec<MagicSchool> CODEC = StringRepresentable.fromValues(MagicSchool::values);
    public static final StreamCodec<io.netty.buffer.ByteBuf, MagicSchool> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], MagicSchool::ordinal);

    private final String id;
    public final int colorArgb;
    public final ChatFormatting chatColor;
    public final String ruName;
    public final String enName;
    /** Ability slot 0 (unlocked from level 1). */
    public final String ability1Name;
    /** Ability slot 1 (unlocked at level 25). */
    public final String ability2Name;
    /** Ability slot 2 (unlocked at level 50). */
    public final String ultimateName;

    MagicSchool(String id, int colorArgb, ChatFormatting chatColor,
                String ruName, String enName,
                String a1, String a2, String ult) {
        this.id = id;
        this.colorArgb = colorArgb;
        this.chatColor = chatColor;
        this.ruName = ruName;
        this.enName = enName;
        this.ability1Name = a1;
        this.ability2Name = a2;
        this.ultimateName = ult;
    }

    @Override
    public String getSerializedName() { return id; }

    public String abilityName(int slot) {
        return switch (slot) {
            case 0 -> ability1Name;
            case 1 -> ability2Name;
            case 2 -> ultimateName;
            default -> "?";
        };
    }
}
