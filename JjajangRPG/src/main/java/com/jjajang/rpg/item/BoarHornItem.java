package com.jjajang.rpg.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BoarHornItem {
    private static NamespacedKey KEY;
    public static void init(JavaPlugin plugin) { KEY = new NamespacedKey(plugin, "boar_horn"); }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.BONE, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "멧돼지 뿔");
        meta.setCustomModelData(1004);
        meta.setLore(List.of(
            ChatColor.GRAY + "멧돼지 김상혁의 날카로운 뿔.",
            ChatColor.DARK_GRAY + "★ 보스 드롭"
        ));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean is(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }
}
