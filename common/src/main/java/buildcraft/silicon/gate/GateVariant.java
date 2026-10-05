/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** The kind of a gate: its logic (and/or), its material (how many slots) and its modifier (how many parameters). */
public record GateVariant(Logic logic, Material material, Modifier modifier) {
    public enum Logic implements StringRepresentable {
        AND,
        OR;

        public static final Codec<Logic> CODEC = StringRepresentable.fromEnum(Logic::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Material implements StringRepresentable {
        CLAY_BRICK(1, false),
        IRON(2, true),
        NETHER_BRICK(4, true),
        GOLD(8, true);

        public static final Codec<Material> CODEC = StringRepresentable.fromEnum(Material::values);
        public final int numSlots;
        public final boolean canBeModified;

        Material(int numSlots, boolean canBeModified) {
            this.numSlots = numSlots;
            this.canBeModified = canBeModified;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Modifier implements StringRepresentable {
        NO_MODIFIER(0, 0, 1),
        LAPIS(1, 0, 1),
        QUARTZ(1, 1, 2),
        DIAMOND(3, 3, 2);

        public static final Codec<Modifier> CODEC = StringRepresentable.fromEnum(Modifier::values);
        public final int triggerParams, actionParams, slotDivisor;

        Modifier(int triggerParams, int actionParams, int slotDivisor) {
            this.triggerParams = triggerParams;
            this.actionParams = actionParams;
            this.slotDivisor = slotDivisor;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final GateVariant BASIC = new GateVariant(Logic.AND, Material.CLAY_BRICK, Modifier.NO_MODIFIER);

    public static final Codec<GateVariant> CODEC = RecordCodecBuilder.create(i -> i.group(
        Logic.CODEC.fieldOf("logic").forGetter(GateVariant::logic),
        Material.CODEC.fieldOf("material").forGetter(GateVariant::material),
        Modifier.CODEC.fieldOf("modifier").forGetter(GateVariant::modifier)).apply(i, GateVariant::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GateVariant> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.map(i -> Logic.values()[i], Logic::ordinal), GateVariant::logic,
        ByteBufCodecs.VAR_INT.map(i -> Material.values()[i], Material::ordinal), GateVariant::material,
        ByteBufCodecs.VAR_INT.map(i -> Modifier.values()[i], Modifier::ordinal), GateVariant::modifier,
        GateVariant::new);

    public int numSlots() {
        return Math.max(1, material.numSlots / modifier.slotDivisor);
    }

    public int numTriggerParams() {
        return modifier.triggerParams;
    }

    public int numActionParams() {
        return modifier.actionParams;
    }

    /** @return A name for models and recipes, such as "iron_and_lapis", or "clay_brick" for the basic gate. */
    public String getName() {
        if (!material.canBeModified) return material.getSerializedName();
        return material.getSerializedName() + "_" + logic.getSerializedName() + "_" + modifier.getSerializedName();
    }

    public Component getDisplayName() {
        if (!material.canBeModified) {
            return Component.translatable("gate.buildcraft.name.basic");
        }
        Component name = Component.translatable("gate.buildcraft.name", Component.translatable("gate.buildcraft.material." + material.getSerializedName()),
            Component.translatable("gate.buildcraft.logic." + logic.getSerializedName()));
        if (modifier == Modifier.NO_MODIFIER) return name;
        return Component.translatable("gate.buildcraft.name.modified", name,
            Component.translatable("gate.buildcraft.modifier." + modifier.getSerializedName()));
    }

    /** @return Every variant a gate can be. */
    public static List<GateVariant> all() {
        List<GateVariant> list = new ArrayList<>();
        list.add(BASIC);
        for (Material material : Material.values()) {
            if (!material.canBeModified) continue;
            for (Logic logic : Logic.values()) {
                for (Modifier modifier : Modifier.values()) {
                    list.add(new GateVariant(logic, material, modifier));
                }
            }
        }
        return list;
    }
}
