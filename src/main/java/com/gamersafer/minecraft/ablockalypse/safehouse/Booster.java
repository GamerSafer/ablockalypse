package com.gamersafer.minecraft.ablockalypse.safehouse;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public enum Booster {

    SPEED(51692, PotionEffectType.SPEED),
    STRENGTH(44573, PotionEffectType.INCREASE_DAMAGE),
    RESISTANCE(24906, PotionEffectType.DAMAGE_RESISTANCE),
    REGENERATION(2429, PotionEffectType.REGENERATION),
    JUMP_BOOST(46224, PotionEffectType.JUMP),
    LESS_HUNGER(55654),
    LESS_THIRST(34719),
    NIGHT_VISION(26497, PotionEffectType.NIGHT_VISION),
    LUCK(41568, PotionEffectType.LUCK),
    KNOCKBACK(41633);

    /**
     * See {@link #getPermission()}
     */
    private final String permission;

    /**
     * See {@link #getDisplayName()}
     */
    private final String displayName;

    /**
     * See {@link #getHdbId()}
     */
    private final int hdbId;

    /**
     * The potion effect type given by this booster.
     * It might be null.
     */
    @Nullable
    private final PotionEffectType potionEffectType;

    /**
     * See {@link #getHead()}
     */
    private ItemStack head;

    Booster(int hdbId, @Nullable PotionEffectType potionEffectType) {
        this.permission = "ablockalypse.booster." + name().toLowerCase();
        this.displayName = WordUtils.capitalize(name().toLowerCase().replace("_", " "));
        this.hdbId = hdbId;
        this.potionEffectType = potionEffectType;
    }

    Booster(int hdbId) {
        this(hdbId, null);
    }

    /**
     * @return the permission required to activate this booster
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the display name of this booster.
     *
     * @return the capitalized name of this booster
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the HeadDatabase ID of head representing this booster in menus.
     *
     * @return the head id
     * @see #getHead()
     */
    public int getHdbId() {
        return hdbId;
    }

    /**
     * Gets the head representing this booster in menus.
     *
     * @return the head item, or a regular player head if the head couldn't be loaded
     * @see #getHdbId()
     */
    public ItemStack getHead() {
        if (head == null) {
            return new ItemStack(Material.PLAYER_HEAD);
        }
        return head;
    }

    /**
     * Sets the head representing this booster in menus.
     *
     * @param head the head item
     */
    public void setHead(ItemStack head) {
        this.head = head;
    }

    /**
     * Gives this booster potion effect to the given player.
     * If the player has an active higher potion effect of the same type, it won't be replaced.
     * If there isn't a potion effect type for this booster, nothing happens.
     *
     * @param player who should get the potion effect
     */
    void give(Player player) {
        if (potionEffectType != null) {
            boolean hasHigherAmplifier = player.getActivePotionEffects().stream()
                    .filter(potionEffect -> potionEffect.getType() == potionEffectType)
                    .anyMatch(potionEffect -> potionEffect.getAmplifier() > 1);
            if (!hasHigherAmplifier) {
                player.addPotionEffect(new PotionEffect(potionEffectType, 20 * 6, 1));
            }
        }
    }

}
