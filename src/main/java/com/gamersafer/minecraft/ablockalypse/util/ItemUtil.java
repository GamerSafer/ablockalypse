package com.gamersafer.minecraft.ablockalypse.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ItemUtil {

    public static final Set<Material> WEAPONS = EnumSet.of(
            Material.TRIDENT,
            Material.WOODEN_AXE,
            Material.WOODEN_SWORD,
            Material.WOODEN_PICKAXE, //Crossbow
            Material.DIAMOND_SWORD,
            Material.DIAMOND_AXE,
            Material.BOW,
            Material.CROSSBOW,
            Material.IRON_AXE,
            Material.IRON_SWORD,
            Material.IRON_HORSE_ARMOR, //Musket
            Material.STICK //Lance
    );

    public static final Set<Material> ARMOR = EnumSet.of(
            Material.CHAINMAIL_BOOTS,
            Material.CHAINMAIL_LEGGINGS,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_HELMET,
            Material.IRON_BOOTS,
            Material.IRON_LEGGINGS,
            Material.IRON_CHESTPLATE,
            Material.IRON_HELMET,
            Material.LEATHER_BOOTS,
            Material.LEATHER_LEGGINGS,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_HELMET,
            Material.DIAMOND_BOOTS,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_HELMET,
            Material.TURTLE_HELMET,
            Material.GOLDEN_BOOTS,
            Material.GOLDEN_LEGGINGS,
            Material.GOLDEN_CHESTPLATE,
            Material.GOLDEN_HELMET
    );

    private ItemUtil() {
        // prevent initialization
    }

    public static ItemStack createPlayerHead(String textureUrl, int amount, String displayName, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, amount);

        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        profile.getProperties().put("textures", new Property("textures", textureUrl));
        try {
            Field profileField = headMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(headMeta, profile);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        head.setItemMeta(headMeta);

        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(displayName);
        if (lore != null) meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack result = new ItemStack(material);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(FormatUtil.color(name));
        meta.setLore(FormatUtil.color(lore));
        result.setItemMeta(meta);
        return result;
    }

    public static void addItemGlow(ItemStack item) {
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS); //If you want to hide the enchantment
        item.setItemMeta(itemMeta);
    }

}
