package com.jjajang.rpg.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PlatinumJjajangItem {

    private static NamespacedKey KEY;

    public static void init(JavaPlugin plugin) {
        KEY = new NamespacedKey(plugin, "platinum_jjajang");
    }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.BROWN_WOOL, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ 플래티넘 짜장 ✦");
        meta.setLore(List.of(
                ChatColor.GRAY + "신비로운 짜장 박스",
                ChatColor.YELLOW + "우클릭으로 오픈 UI를 엽니다",
                ChatColor.DARK_PURPLE + "경험치 쿠폰, 골드 등 보상 획득!"
        ));
        meta.setCustomModelData(1008);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean is(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    public static NamespacedKey getKey() { return KEY; }
}
