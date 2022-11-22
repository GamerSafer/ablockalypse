package com.gamersafer.minecraft.ablockalypse.safehouse;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public enum Booster {

    SPEED(PotionEffectType.SPEED),
    STRENGTH(PotionEffectType.INCREASE_DAMAGE),
    RESISTANCE(PotionEffectType.DAMAGE_RESISTANCE),
    REGENERATION(PotionEffectType.REGENERATION),
    JUMP_BOOST(PotionEffectType.JUMP),
    LESS_HUNGER,
    LESS_THIRST,
    NIGHT_VISION(PotionEffectType.NIGHT_VISION),
    LUCK(PotionEffectType.LUCK),
    KNOCKBACK;

    private final String permission;
    @Nullable
    private final PotionEffectType potionEffectType;

    Booster(@Nullable PotionEffectType potionEffectType) {
        this.permission = "ablockalypse.booster." + name().toLowerCase();
        this.potionEffectType = potionEffectType;
    }

    Booster() {
        this(null);
    }

    public String getPermission() {
        return permission;
    }

    void give(Player player) {
        if (potionEffectType != null) {
            player.addPotionEffect(new PotionEffect(potionEffectType, 20 * 6, 1));
        }
    }

}
