package com.jjajang.rpg.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class BoarMeatItem {
    private static NamespacedKey KEY;
    public static void init(JavaPlugin plugin) { KEY = new NamespacedKey(plugin, "boar_meat"); }

    public static ItemStack create(int amount) {
        ItemStack item = new ItemStack(Material.PORKCHOP, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "멧돼지고기");
        meta.setCustomModelData(1006);
        meta.setLore(List.of(
            ChatColor.GRAY + "멧돼지 김상혁의 고기.",
            ChatColor.YELLOW + "배고플 때 먹으면 좋을 것 같다.",
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
